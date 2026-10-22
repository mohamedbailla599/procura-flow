package com.procuraflow.fournisseurs.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.procuraflow.fournisseurs.entity.Fournisseur;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Schema(description = "Représentation publique d'un fournisseur")
public record FournisseurResponse(

        @Schema(description = "Identifiant unique du fournisseur", example = "12")
        Long id,

        @Schema(description = "Nom du fournisseur", example = "Atlas Fournitures")
        String nom,

        @Schema(description = "Adresse e-mail unique normalisée", example = "contact@atlas.example")
        String email,

        @Schema(description = "Numéro de téléphone", example = "+212600000000")
        String telephone,

        @Schema(description = "Adresse postale", example = "Agadir, Maroc")
        String adresse,

        @Schema(description = "Indique si le fournisseur est actif pour de nouvelles commandes", example = "true")
        Boolean actif,

        @Schema(description = "Date de création (ISO-8601 UTC)", example = "2026-10-21T13:30:00Z")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
        OffsetDateTime createdAt,

        @Schema(description = "Date de dernière modification (ISO-8601 UTC)", example = "2026-10-21T13:30:00Z")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
        OffsetDateTime updatedAt
) {

    public static FournisseurResponse fromEntity(Fournisseur entity) {
        return new FournisseurResponse(
                entity.getId(),
                entity.getNom(),
                entity.getEmail(),
                entity.getTelephone(),
                entity.getAdresse(),
                entity.getActif(),
                normalizeUtc(entity.getCreatedAt()),
                normalizeUtc(entity.getUpdatedAt())
        );
    }

    private static OffsetDateTime normalizeUtc(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }
}
