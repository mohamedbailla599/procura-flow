package com.procuraflow.fournisseurs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.procuraflow.fournisseurs.client.CommandesClient;
import com.procuraflow.fournisseurs.dto.CommandeSummaryResponse;
import com.procuraflow.fournisseurs.dto.FournisseurRequest;
import com.procuraflow.fournisseurs.dto.FournisseurResponse;
import com.procuraflow.fournisseurs.entity.Fournisseur;
import com.procuraflow.fournisseurs.exception.DuplicateEmailException;
import com.procuraflow.fournisseurs.exception.FournisseurNotFoundException;
import com.procuraflow.fournisseurs.repository.FournisseurRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class FournisseurServiceTest {

    @Mock
    private FournisseurRepository fournisseurRepository;

    @Mock
    private CommandesClient commandesClient;

    @InjectMocks
    private FournisseurService fournisseurService;

    @Test
    void createFournisseur_shouldNormalizeEmailAndSetActifTrue() {
        FournisseurRequest request = new FournisseurRequest(
                "  Atlas Fournitures  ",
                "  Contact@Atlas.Example ",
                "+212600000000",
                "Agadir, Maroc"
        );

        when(fournisseurRepository.existsByEmailIgnoreCase("contact@atlas.example")).thenReturn(false);
        when(fournisseurRepository.saveAndFlush(any(Fournisseur.class))).thenAnswer(invocation -> {
            Fournisseur f = invocation.getArgument(0);
            f.setId(12L);
            f.onPrePersist();
            return f;
        });

        FournisseurResponse response = fournisseurService.createFournisseur(request);

        assertThat(response.id()).isEqualTo(12L);
        assertThat(response.nom()).isEqualTo("Atlas Fournitures");
        assertThat(response.email()).isEqualTo("contact@atlas.example");
        assertThat(response.actif()).isTrue();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();

        ArgumentCaptor<Fournisseur> captor = ArgumentCaptor.forClass(Fournisseur.class);
        verify(fournisseurRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getActif()).isTrue();
    }

    @Test
    void createFournisseur_shouldThrowDuplicateEmailExceptionWhenEmailExists() {
        FournisseurRequest request = new FournisseurRequest(
                "Atlas Fournitures",
                "contact@atlas.example",
                null,
                null
        );

        when(fournisseurRepository.existsByEmailIgnoreCase("contact@atlas.example")).thenReturn(true);

        assertThatThrownBy(() -> fournisseurService.createFournisseur(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("contact@atlas.example");

        verify(fournisseurRepository, never()).saveAndFlush(any());
    }

    @Test
    void getFournisseurById_shouldReturnSupplierWhenExists() {
        Fournisseur entity = buildEntity(5L, "Maroc Bureau", "info@marocbureau.ma", true);
        when(fournisseurRepository.findById(5L)).thenReturn(Optional.of(entity));

        FournisseurResponse response = fournisseurService.getFournisseurById(5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.nom()).isEqualTo("Maroc Bureau");
        assertThat(response.email()).isEqualTo("info@marocbureau.ma");
    }

    @Test
    void getFournisseurById_shouldThrowNotFoundWhenMissing() {
        when(fournisseurRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fournisseurService.getFournisseurById(999L))
                .isInstanceOf(FournisseurNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void updateFournisseur_shouldUpdateFieldsAndPreserveActifStatus() {
        Fournisseur existing = buildEntity(10L, "Ancien Nom", "ancien@example.com", false);
        FournisseurRequest updateRequest = new FournisseurRequest(
                "Nouveau Nom",
                "NOUVEAU@example.com",
                "+212528000000",
                "Casablanca"
        );

        when(fournisseurRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(fournisseurRepository.existsByEmailIgnoreCaseAndIdNot("nouveau@example.com", 10L)).thenReturn(false);
        when(fournisseurRepository.saveAndFlush(any(Fournisseur.class))).thenAnswer(inv -> {
            Fournisseur f = inv.getArgument(0);
            f.onPreUpdate();
            return f;
        });

        FournisseurResponse updated = fournisseurService.updateFournisseur(10L, updateRequest);

        assertThat(updated.id()).isEqualTo(10L);
        assertThat(updated.nom()).isEqualTo("Nouveau Nom");
        assertThat(updated.email()).isEqualTo("nouveau@example.com");
        assertThat(updated.telephone()).isEqualTo("+212528000000");
        assertThat(updated.adresse()).isEqualTo("Casablanca");
        assertThat(updated.actif()).isFalse();
    }

    @Test
    void updateFournisseur_shouldRejectDuplicateEmailBelongingToAnotherSupplier() {
        Fournisseur existing = buildEntity(10L, "Nom", "actuel@example.com", true);
        FournisseurRequest updateRequest = new FournisseurRequest(
                "Nom",
                "pris@example.com",
                null,
                null
        );

        when(fournisseurRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(fournisseurRepository.existsByEmailIgnoreCaseAndIdNot("pris@example.com", 10L)).thenReturn(true);

        assertThatThrownBy(() -> fournisseurService.updateFournisseur(10L, updateRequest))
                .isInstanceOf(DuplicateEmailException.class);

        verify(fournisseurRepository, never()).saveAndFlush(any());
    }

    @Test
    void deactivateFournisseur_shouldPerformSoftDeactivationWithoutDeletingRecord() {
        Fournisseur existing = buildEntity(7L, "Souss Equipement", "contact@souss.ma", true);
        when(fournisseurRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(fournisseurRepository.saveAndFlush(any(Fournisseur.class))).thenAnswer(inv -> inv.getArgument(0));

        FournisseurResponse deactivated = fournisseurService.deactivateFournisseur(7L);

        assertThat(deactivated.id()).isEqualTo(7L);
        assertThat(deactivated.actif()).isFalse();
        verify(fournisseurRepository, never()).delete(any(Fournisseur.class));
        verify(fournisseurRepository, never()).deleteById(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listFournisseurs_shouldReturnMappedSuppliers() {
        Fournisseur f1 = buildEntity(1L, "Atlas", "a@atlas.ma", true);
        when(fournisseurRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(f1));

        List<FournisseurResponse> result = fournisseurService.listFournisseurs(true, "Atlas", null, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nom()).isEqualTo("Atlas");
    }

    @Test
    void getCommandesByFournisseurId_shouldVerifySupplierExistsAndCallCommandesClient() {
        when(fournisseurRepository.existsById(12L)).thenReturn(true);
        List<CommandeSummaryResponse> expectedOrders = List.of(
                new CommandeSummaryResponse(101L, 12L, "2026-10-15T10:30:00Z", "VALIDEE", new BigDecimal("450.00"))
        );
        when(commandesClient.getCommandesByFournisseurId(12L)).thenReturn(expectedOrders);

        List<CommandeSummaryResponse> result = fournisseurService.getCommandesByFournisseurId(12L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(101L);
        assertThat(result.get(0).fournisseurId()).isEqualTo(12L);
        verify(commandesClient).getCommandesByFournisseurId(eq(12L));
    }

    @Test
    void getCommandesByFournisseurId_shouldThrowNotFoundAndNotCallDownstreamWhenSupplierMissing() {
        when(fournisseurRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> fournisseurService.getCommandesByFournisseurId(404L))
                .isInstanceOf(FournisseurNotFoundException.class);

        verify(commandesClient, never()).getCommandesByFournisseurId(any());
    }

    private static Fournisseur buildEntity(Long id, String nom, String email, boolean actif) {
        Fournisseur f = new Fournisseur(nom, email, "+212600000000", "Agadir");
        f.setId(id);
        f.setActif(actif);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        f.setCreatedAt(now);
        f.setUpdatedAt(now);
        return f;
    }
}
