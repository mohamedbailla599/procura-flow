package com.procuraflow.fournisseurs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ligne d'une commande retournée par le service Commandes")
public record LigneCommandeSummaryDto(
        Long id,
        String designation,
        Integer quantite,
        BigDecimal prixUnitaire
) {
}
