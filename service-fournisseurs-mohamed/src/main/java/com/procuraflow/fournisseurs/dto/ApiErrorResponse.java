package com.procuraflow.fournisseurs.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Format d'erreur standardisé ProcuraFlow")
public record ApiErrorResponse(

        @Schema(description = "Horodatage UTC de l'erreur", example = "2026-10-21T13:30:00Z")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
        OffsetDateTime timestamp,

        @Schema(description = "Code de statut HTTP", example = "404")
        int status,

        @Schema(description = "Code d'erreur normalisé", example = "NOT_FOUND")
        String error,

        @Schema(description = "Message explicatif destiné au client", example = "Fournisseur introuvable avec l'identifiant : 42")
        String message,

        @Schema(description = "Chemin de la requête HTTP", example = "/api/fournisseurs/42")
        String path,

        @Schema(description = "Détails des erreurs de validation par champ (optionnel)")
        Map<String, String> validationErrors
) {

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS),
                status,
                error,
                message,
                path,
                null
        );
    }

    public static ApiErrorResponse ofValidation(
            int status,
            String error,
            String message,
            String path,
            Map<String, String> validationErrors
    ) {
        return new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS),
                status,
                error,
                message,
                path,
                validationErrors
        );
    }
}
