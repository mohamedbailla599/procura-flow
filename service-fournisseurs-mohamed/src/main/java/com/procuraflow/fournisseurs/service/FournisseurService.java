package com.procuraflow.fournisseurs.service;

import com.procuraflow.fournisseurs.client.CommandesClient;
import com.procuraflow.fournisseurs.dto.CommandeSummaryResponse;
import com.procuraflow.fournisseurs.dto.FournisseurRequest;
import com.procuraflow.fournisseurs.dto.FournisseurResponse;
import com.procuraflow.fournisseurs.entity.Fournisseur;
import com.procuraflow.fournisseurs.exception.DuplicateEmailException;
import com.procuraflow.fournisseurs.exception.FournisseurNotFoundException;
import com.procuraflow.fournisseurs.repository.FournisseurRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FournisseurService {

    private final FournisseurRepository fournisseurRepository;
    private final CommandesClient commandesClient;

    public FournisseurService(
            FournisseurRepository fournisseurRepository,
            CommandesClient commandesClient
    ) {
        this.fournisseurRepository = fournisseurRepository;
        this.commandesClient = commandesClient;
    }

    @Transactional
    public FournisseurResponse createFournisseur(FournisseurRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (fournisseurRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        Fournisseur fournisseur = new Fournisseur(
                request.nom().trim(),
                normalizedEmail,
                request.telephone(),
                request.adresse()
        );
        fournisseur.setActif(Boolean.TRUE);

        Fournisseur saved = fournisseurRepository.saveAndFlush(fournisseur);
        return FournisseurResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<FournisseurResponse> listFournisseurs(
            Boolean actif,
            String nom,
            String email,
            Boolean includeInactive
    ) {
        Specification<Fournisseur> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (actif != null) {
                predicates.add(cb.equal(root.get("actif"), actif));
            } else if (!Boolean.TRUE.equals(includeInactive)) {
                // Per integration-contract.md Section 5.4: GET /api/fournisseurs excludes inactive suppliers by default
                // unless ?actif=false or ?includeInactive=true is explicitly provided.
                predicates.add(cb.isTrue(root.get("actif")));
            }

            if (StringUtils.hasText(nom)) {
                String pattern = "%" + escapeLikePattern(nom.trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.like(cb.lower(root.get("nom")), pattern, '\\'));
            }

            if (StringUtils.hasText(email)) {
                String pattern = "%" + escapeLikePattern(email.trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.like(cb.lower(root.get("email")), pattern, '\\'));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return fournisseurRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(FournisseurResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public FournisseurResponse getFournisseurById(Long id) {
        Fournisseur fournisseur = findEntityOrThrow(id);
        return FournisseurResponse.fromEntity(fournisseur);
    }

    @Transactional
    public FournisseurResponse updateFournisseur(Long id, FournisseurRequest request) {
        Fournisseur existing = findEntityOrThrow(id);
        String normalizedEmail = normalizeEmail(request.email());

        if (fournisseurRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        existing.setNom(request.nom().trim());
        existing.setEmail(normalizedEmail);
        existing.setTelephone(request.telephone());
        existing.setAdresse(request.adresse());

        Fournisseur updated = fournisseurRepository.saveAndFlush(existing);
        return FournisseurResponse.fromEntity(updated);
    }

    @Transactional
    public FournisseurResponse deactivateFournisseur(Long id) {
        Fournisseur existing = findEntityOrThrow(id);
        existing.setActif(Boolean.FALSE);
        Fournisseur updated = fournisseurRepository.saveAndFlush(existing);
        return FournisseurResponse.fromEntity(updated);
    }

    @Transactional
    public FournisseurResponse reactivateFournisseur(Long id) {
        Fournisseur existing = findEntityOrThrow(id);
        existing.setActif(Boolean.TRUE);
        Fournisseur updated = fournisseurRepository.saveAndFlush(existing);
        return FournisseurResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public List<CommandeSummaryResponse> getCommandesByFournisseurId(Long id) {
        if (!fournisseurRepository.existsById(id)) {
            throw new FournisseurNotFoundException(id);
        }
        return commandesClient.getCommandesByFournisseurId(id);
    }

    private Fournisseur findEntityOrThrow(Long id) {
        return fournisseurRepository.findById(id)
                .orElseThrow(() -> new FournisseurNotFoundException(id));
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String escapeLikePattern(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
