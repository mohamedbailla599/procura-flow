package com.procuraflow.fournisseurs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Commande d'achat associée à un fournisseur (récupérée via le service Commandes)")
public record CommandeSummaryResponse(

        @Schema(description = "Identifiant de la commande", example = "101")
        Long id,

        @Schema(description = "Identifiant du fournisseur associé", example = "12")
        Long fournisseurId,

        @Schema(description = "Date de la commande (ISO-8601)", example = "2026-10-15T10:30:00Z")
        String dateCommande,

        @Schema(description = "Statut de la commande", example = "VALIDEE")
        String statut,

        @Schema(description = "Montant total de la commande", example = "450.00")
        BigDecimal montantTotal,

        @Schema(description = "Lignes de commande éventuelles si fournies par le service Commandes")
        List<LigneCommandeSummaryDto> lignes
) {
    public CommandeSummaryResponse(
            Long id,
            Long fournisseurId,
            String dateCommande,
            String statut,
            BigDecimal montantTotal
    ) {
        this(id, fournisseurId, dateCommande, statut, montantTotal, null);
    }
}
