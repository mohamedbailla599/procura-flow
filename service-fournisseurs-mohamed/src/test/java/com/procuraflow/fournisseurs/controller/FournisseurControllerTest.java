package com.procuraflow.fournisseurs.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procuraflow.fournisseurs.dto.CommandeSummaryResponse;
import com.procuraflow.fournisseurs.dto.FournisseurRequest;
import com.procuraflow.fournisseurs.dto.FournisseurResponse;
import com.procuraflow.fournisseurs.exception.DownstreamBadGatewayException;
import com.procuraflow.fournisseurs.exception.DownstreamServiceUnavailableException;
import com.procuraflow.fournisseurs.exception.DuplicateEmailException;
import com.procuraflow.fournisseurs.exception.FournisseurNotFoundException;
import com.procuraflow.fournisseurs.exception.GlobalExceptionHandler;
import com.procuraflow.fournisseurs.service.FournisseurService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = FournisseurController.class)
@Import(GlobalExceptionHandler.class)
class FournisseurControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FournisseurService fournisseurService;

    private static final OffsetDateTime SAMPLE_TIME =
            OffsetDateTime.of(2026, 10, 21, 13, 30, 0, 0, ZoneOffset.UTC);

    @Test
    void createFournisseur_shouldReturn201AndLocationHeader() throws Exception {
        FournisseurRequest request = new FournisseurRequest(
                "Atlas Fournitures",
                "contact@atlas.example",
                "+212600000000",
                "Agadir, Maroc"
        );
        FournisseurResponse created = new FournisseurResponse(
                12L,
                "Atlas Fournitures",
                "contact@atlas.example",
                "+212600000000",
                "Agadir, Maroc",
                true,
                SAMPLE_TIME,
                SAMPLE_TIME
        );

        when(fournisseurService.createFournisseur(any(FournisseurRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/fournisseurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/fournisseurs/12"))
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.nom").value("Atlas Fournitures"))
                .andExpect(jsonPath("$.email").value("contact@atlas.example"))
                .andExpect(jsonPath("$.actif").value(true))
                .andExpect(jsonPath("$.createdAt").value("2026-10-21T13:30:00Z"));
    }

    @Test
    void createFournisseur_shouldReturn400OnInvalidEmailAndBlankName() throws Exception {
        FournisseurRequest invalidRequest = new FournisseurRequest(
                "   ",
                "not-an-email",
                "+212600000000",
                "Agadir"
        );

        mockMvc.perform(post("/api/fournisseurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/fournisseurs"))
                .andExpect(jsonPath("$.validationErrors.nom").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    void createFournisseur_shouldReturn409OnDuplicateEmail() throws Exception {
        FournisseurRequest request = new FournisseurRequest(
                "Atlas Fournitures",
                "contact@atlas.example",
                null,
                null
        );

        when(fournisseurService.createFournisseur(any(FournisseurRequest.class)))
                .thenThrow(new DuplicateEmailException("contact@atlas.example"));

        mockMvc.perform(post("/api/fournisseurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.path").value("/api/fournisseurs"));
    }

    @Test
    void listFournisseurs_shouldReturn200WithJsonArray() throws Exception {
        FournisseurResponse item = new FournisseurResponse(
                12L,
                "Atlas Fournitures",
                "contact@atlas.example",
                "+212600000000",
                "Agadir, Maroc",
                true,
                SAMPLE_TIME,
                SAMPLE_TIME
        );

        when(fournisseurService.listFournisseurs(true, "Atlas", null, false))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/fournisseurs")
                        .param("actif", "true")
                        .param("nom", "Atlas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[0].nom").value("Atlas Fournitures"));
    }

    @Test
    void getFournisseurById_shouldReturn404WhenMissing() throws Exception {
        when(fournisseurService.getFournisseurById(42L))
                .thenThrow(new FournisseurNotFoundException(42L));

        mockMvc.perform(get("/api/fournisseurs/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/fournisseurs/42"));
    }

    @Test
    void getFournisseurById_shouldReturn400OnMalformedId() throws Exception {
        mockMvc.perform(get("/api/fournisseurs/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void updateFournisseur_shouldReturn200WithUpdatedResource() throws Exception {
        FournisseurRequest request = new FournisseurRequest(
                "Atlas Fournitures Pro",
                "pro@atlas.example",
                "+212611111111",
                "Rabat, Maroc"
        );
        FournisseurResponse updated = new FournisseurResponse(
                12L,
                "Atlas Fournitures Pro",
                "pro@atlas.example",
                "+212611111111",
                "Rabat, Maroc",
                true,
                SAMPLE_TIME,
                SAMPLE_TIME.plusHours(1)
        );

        when(fournisseurService.updateFournisseur(eq(12L), any(FournisseurRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/fournisseurs/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.nom").value("Atlas Fournitures Pro"))
                .andExpect(jsonPath("$.email").value("pro@atlas.example"));
    }

    @Test
    void deactivateFournisseurPatch_shouldReturn200WithActifFalse() throws Exception {
        FournisseurResponse deactivated = new FournisseurResponse(
                12L,
                "Atlas Fournitures",
                "contact@atlas.example",
                "+212600000000",
                "Agadir, Maroc",
                false,
                SAMPLE_TIME,
                SAMPLE_TIME.plusMinutes(15)
        );

        when(fournisseurService.deactivateFournisseur(12L)).thenReturn(deactivated);

        mockMvc.perform(patch("/api/fournisseurs/12/desactiver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.actif").value(false));
    }

    @Test
    void deactivateFournisseurDelete_shouldReturn204NoContent() throws Exception {
        FournisseurResponse deactivated = new FournisseurResponse(
                12L,
                "Atlas Fournitures",
                "contact@atlas.example",
                null,
                null,
                false,
                SAMPLE_TIME,
                SAMPLE_TIME
        );
        when(fournisseurService.deactivateFournisseur(12L)).thenReturn(deactivated);

        mockMvc.perform(delete("/api/fournisseurs/12"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getFournisseurCommandes_shouldReturn200WithOrderHistory() throws Exception {
        List<CommandeSummaryResponse> orders = List.of(
                new CommandeSummaryResponse(101L, 12L, "2026-10-15T10:30:00Z", "VALIDEE", new BigDecimal("450.00")),
                new CommandeSummaryResponse(108L, 12L, "2026-10-19T14:00:00Z", "EN_ATTENTE", new BigDecimal("1250.00"))
        );

        when(fournisseurService.getCommandesByFournisseurId(12L)).thenReturn(orders);

        mockMvc.perform(get("/api/fournisseurs/12/commandes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].fournisseurId").value(12))
                .andExpect(jsonPath("$[0].statut").value("VALIDEE"))
                .andExpect(jsonPath("$[0].montantTotal").value(450.00));
    }

    @Test
    void getFournisseurCommandes_shouldReturn503WhenCommandesServiceUnavailable() throws Exception {
        when(fournisseurService.getCommandesByFournisseurId(12L))
                .thenThrow(new DownstreamServiceUnavailableException(
                        "Le service Commandes est temporairement indisponible."
                ));

        mockMvc.perform(get("/api/fournisseurs/12/commandes"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.path").value("/api/fournisseurs/12/commandes"));
    }

    @Test
    void getFournisseurCommandes_shouldReturn502WhenCommandesReturnsInvalidResponse() throws Exception {
        when(fournisseurService.getCommandesByFournisseurId(12L))
                .thenThrow(new DownstreamBadGatewayException(
                        "Réponse invalide reçue depuis le service Commandes."
                ));

        mockMvc.perform(get("/api/fournisseurs/12/commandes"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("BAD_GATEWAY"))
                .andExpect(jsonPath("$.path").value("/api/fournisseurs/12/commandes"));
    }
}
