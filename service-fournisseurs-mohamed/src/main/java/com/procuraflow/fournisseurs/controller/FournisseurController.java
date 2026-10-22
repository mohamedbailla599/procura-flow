package com.procuraflow.fournisseurs.controller;

import com.procuraflow.fournisseurs.dto.ApiErrorResponse;
import com.procuraflow.fournisseurs.dto.CommandeSummaryResponse;
import com.procuraflow.fournisseurs.dto.FournisseurRequest;
import com.procuraflow.fournisseurs.dto.FournisseurResponse;
import com.procuraflow.fournisseurs.service.FournisseurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/api/fournisseurs", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Fournisseurs", description = "Gestion des fournisseurs et consultation de leur historique de commandes")
public class FournisseurController {

    private final FournisseurService fournisseurService;

    public FournisseurController(FournisseurService fournisseurService) {
        this.fournisseurService = fournisseurService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Créer un nouveau fournisseur",
            description = "Crée un fournisseur actif par défaut (actif = true) après validation des champs et vérification de l'unicité de l'e-mail."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fournisseur créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Adresse e-mail déjà utilisée",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<FournisseurResponse> createFournisseur(
            @Valid @RequestBody FournisseurRequest request
    ) {
        FournisseurResponse created = fournisseurService.createFournisseur(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(
            summary = "Lister les fournisseurs",
            description = "Retourne la liste des fournisseurs. Par défaut, seuls les fournisseurs actifs sont retournés (conformément au contrat d'intégration), sauf si actif=false ou includeInactive=true est précisé."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des fournisseurs récupérée avec succès"),
            @ApiResponse(responseCode = "400", description = "Paramètres de filtrage invalides",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<FournisseurResponse>> listFournisseurs(
            @Parameter(description = "Filtrer par statut actif (true ou false)")
            @RequestParam(name = "actif", required = false) Boolean actif,

            @Parameter(description = "Filtrer par nom (recherche partielle insensible à la casse)")
            @RequestParam(name = "nom", required = false) String nom,

            @Parameter(description = "Filtrer par adresse e-mail (recherche partielle insensible à la casse)")
            @RequestParam(name = "email", required = false) String email,

            @Parameter(description = "Inclure les fournisseurs inactifs lorsque le filtre 'actif' n'est pas spécifié")
            @RequestParam(name = "includeInactive", required = false, defaultValue = "false") Boolean includeInactive
    ) {
        List<FournisseurResponse> fournisseurs = fournisseurService.listFournisseurs(
                actif,
                nom,
                email,
                includeInactive
        );
        return ResponseEntity.ok(fournisseurs);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consulter un fournisseur par son identifiant",
            description = "Retourne un fournisseur (qu'il soit actif ou inactif) afin de préserver les références historiques pour le service Commandes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fournisseur trouvé"),
            @ApiResponse(responseCode = "404", description = "Fournisseur introuvable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<FournisseurResponse> getFournisseurById(
            @Parameter(description = "Identifiant du fournisseur", example = "12")
            @PathVariable("id") @Positive(message = "L'identifiant du fournisseur doit être un entier positif.") Long id
    ) {
        return ResponseEntity.ok(fournisseurService.getFournisseurById(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Mettre à jour un fournisseur",
            description = "Met à jour les informations modifiables d'un fournisseur (nom, email, téléphone, adresse) tout en préservant son identifiant, sa date de création et son statut actif/inactif."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fournisseur mis à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Données de requête invalides",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Fournisseur introuvable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Adresse e-mail déjà utilisée par un autre fournisseur",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<FournisseurResponse> updateFournisseur(
            @PathVariable("id") @Positive(message = "L'identifiant du fournisseur doit être un entier positif.") Long id,
            @Valid @RequestBody FournisseurRequest request
    ) {
        return ResponseEntity.ok(fournisseurService.updateFournisseur(id, request));
    }

    @PatchMapping("/{id}/desactiver")
    @Operation(
            summary = "Désactiver logiquement un fournisseur (PATCH)",
            description = "Passe actif à false sans supprimer l'enregistrement en base de données, préservant ainsi l'historique des achats."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fournisseur désactivé avec succès"),
            @ApiResponse(responseCode = "404", description = "Fournisseur introuvable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<FournisseurResponse> deactivateFournisseurPatch(
            @PathVariable("id") @Positive(message = "L'identifiant du fournisseur doit être un entier positif.") Long id
    ) {
        return ResponseEntity.ok(fournisseurService.deactivateFournisseur(id));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Désactiver logiquement un fournisseur (DELETE du contrat d'intégration)",
            description = "Effectue une désactivation logique (actif = false) conformément à la section 5.1 et 5.4 du contrat d'intégration et retourne 204 No Content."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Fournisseur désactivé logiquement (aucune suppression physique)"),
            @ApiResponse(responseCode = "404", description = "Fournisseur introuvable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> deactivateFournisseurDelete(
            @PathVariable("id") @Positive(message = "L'identifiant du fournisseur doit être un entier positif.") Long id
    ) {
        fournisseurService.deactivateFournisseur(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactiver")
    @Operation(
            summary = "Réactiver un fournisseur (extension proposée O4)",
            description = "Réactive un fournisseur précédemment désactivé (actif = true)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fournisseur réactivé avec succès"),
            @ApiResponse(responseCode = "404", description = "Fournisseur introuvable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<FournisseurResponse> reactivateFournisseur(
            @PathVariable("id") @Positive(message = "L'identifiant du fournisseur doit être un entier positif.") Long id
    ) {
        return ResponseEntity.ok(fournisseurService.reactivateFournisseur(id));
    }

    @GetMapping("/{id}/commandes")
    @Operation(
            summary = "Consulter l'historique des commandes d'un fournisseur",
            description = "Vérifie l'existence locale du fournisseur puis interroge le microservice Commandes via REST (GET /api/commandes?fournisseurId={id})."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historique des commandes récupéré avec succès"),
            @ApiResponse(responseCode = "404", description = "Fournisseur introuvable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "Réponse invalide du service Commandes",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Service Commandes temporairement indisponible",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<List<CommandeSummaryResponse>> getFournisseurCommandes(
            @PathVariable("id") @Positive(message = "L'identifiant du fournisseur doit être un entier positif.") Long id
    ) {
        return ResponseEntity.ok(fournisseurService.getCommandesByFournisseurId(id));
    }
}
