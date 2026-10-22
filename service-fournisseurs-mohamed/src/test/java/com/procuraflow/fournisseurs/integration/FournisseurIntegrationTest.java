package com.procuraflow.fournisseurs.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procuraflow.fournisseurs.dto.FournisseurRequest;
import com.procuraflow.fournisseurs.dto.FournisseurResponse;
import com.procuraflow.fournisseurs.repository.FournisseurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FournisseurIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FournisseurRepository fournisseurRepository;

    @BeforeEach
    void cleanDatabase() {
        fournisseurRepository.deleteAll();
    }

    @Test
    void fullSupplierLifecycle_createListUpdateSoftDeactivateAndGracefulDownstreamFailure() throws Exception {
        // 1. Create supplier -> 201 Created, actif = true, normalized email
        FournisseurRequest createReq = new FournisseurRequest(
                "Atlas Fournitures",
                "Contact@Atlas.Example",
                "+212600000000",
                "Agadir, Maroc"
        );

        MvcResult createResult = mockMvc.perform(post("/api/fournisseurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nom").value("Atlas Fournitures"))
                .andExpect(jsonPath("$.email").value("contact@atlas.example"))
                .andExpect(jsonPath("$.actif").value(true))
                .andReturn();

        FournisseurResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                FournisseurResponse.class
        );
        Long id = created.id();
        assertThat(id).isPositive();

        // 2. Duplicate email (case-insensitive) -> 409 Conflict
        FournisseurRequest duplicateReq = new FournisseurRequest(
                "Autre Société",
                "CONTACT@atlas.example",
                null,
                null
        );
        mockMvc.perform(post("/api/fournisseurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"));

        // 3. Update supplier -> 200 OK
        FournisseurRequest updateReq = new FournisseurRequest(
                "Atlas Fournitures Maroc",
                "contact@atlas.example",
                "+212528840000",
                "Zone Industrielle Tassila, Agadir"
        );
        mockMvc.perform(put("/api/fournisseurs/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Atlas Fournitures Maroc"))
                .andExpect(jsonPath("$.telephone").value("+212528840000"));

        // 4. Soft-deactivate via PATCH /api/fournisseurs/{id}/desactiver -> 200 OK, actif = false
        mockMvc.perform(patch("/api/fournisseurs/{id}/desactiver", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.actif").value(false));

        // 5. Historical lookup by ID still returns deactivated supplier with actif = false
        mockMvc.perform(get("/api/fournisseurs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.actif").value(false));

        // 6. Default list excludes inactive suppliers, while ?actif=false or ?includeInactive=true includes them
        mockMvc.perform(get("/api/fournisseurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/fournisseurs").param("actif", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(get("/api/fournisseurs").param("includeInactive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 7. Reactivate and soft-deactivate via DELETE /api/fournisseurs/{id} -> 204 No Content
        mockMvc.perform(patch("/api/fournisseurs/{id}/reactiver", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(true));

        mockMvc.perform(delete("/api/fournisseurs/{id}", id))
                .andExpect(status().isNoContent());

        assertThat(fournisseurRepository.findById(id)).isPresent();
        assertThat(fournisseurRepository.findById(id).orElseThrow().getActif()).isFalse();

        // 8. Calling GET /api/fournisseurs/{id}/commandes when Commandes service is not running -> 503 Service Unavailable
        mockMvc.perform(get("/api/fournisseurs/{id}/commandes", id))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"));

        // 9. Health endpoint is UP
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
