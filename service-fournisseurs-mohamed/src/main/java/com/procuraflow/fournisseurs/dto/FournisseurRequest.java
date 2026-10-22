package com.procuraflow.fournisseurs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload de création ou de mise à jour d'un fournisseur")
public record FournisseurRequest(

        @Schema(description = "Raison sociale ou nom du fournisseur", example = "Atlas Fournitures", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Le nom du fournisseur est obligatoire.")
        @Pattern(regexp = "(?s).*\\S.*", message = "Le nom du fournisseur doit contenir au moins un caractère non vide.")
        @Size(max = 150, message = "Le nom du fournisseur ne peut pas dépasser 150 caractères.")
        String nom,

        @Schema(description = "Adresse e-mail unique du fournisseur", example = "contact@atlas.example", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "L'adresse e-mail du fournisseur est obligatoire.")
        @Email(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "Le format de l'adresse e-mail est invalide."
        )
        @Size(max = 150, message = "L'adresse e-mail ne peut pas dépasser 150 caractères.")
        String email,

        @Schema(description = "Numéro de téléphone du fournisseur (optionnel)", example = "+212600000000")
        @Size(max = 30, message = "Le numéro de téléphone ne peut pas dépasser 30 caractères.")
        String telephone,

        @Schema(description = "Adresse postale du fournisseur (optionnelle)", example = "Agadir, Maroc")
        @Size(max = 2000, message = "L'adresse ne peut pas dépasser 2000 caractères.")
        String adresse
) {
}
