package com.procuraflow.fournisseurs.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.procuraflow.fournisseurs.config.CommandesClientProperties;
import com.procuraflow.fournisseurs.config.RestClientConfig;
import com.procuraflow.fournisseurs.dto.CommandeSummaryResponse;
import com.procuraflow.fournisseurs.exception.DownstreamBadGatewayException;
import com.procuraflow.fournisseurs.exception.DownstreamServiceUnavailableException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

@RestClientTest(CommandesClient.class)
@Import(RestClientConfig.class)
@EnableConfigurationProperties(CommandesClientProperties.class)
@TestPropertySource(properties = {
        "app.commandes.base-url=http://service-commandes:8082"
})
class CommandesClientTest {

    @Autowired
    private CommandesClient commandesClient;

    @Autowired
    private MockRestServiceServer mockServer;

    @Test
    void getCommandesByFournisseurId_shouldDeserializeOrderHistoryList() {
        String responseJson = """
                [
                  {
                    "id": 101,
                    "fournisseurId": 12,
                    "dateCommande": "2026-10-15T10:30:00Z",
                    "statut": "VALIDEE",
                    "montantTotal": 450.00
                  },
                  {
                    "id": 108,
                    "fournisseurId": 12,
                    "dateCommande": "2026-10-19T14:00:00Z",
                    "statut": "EN_ATTENTE",
                    "montantTotal": 1250.00
                  }
                ]
                """;

        mockServer.expect(requestTo("http://service-commandes:8082/api/commandes?fournisseurId=12"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<CommandeSummaryResponse> result = commandesClient.getCommandesByFournisseurId(12L);

        mockServer.verify();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(101L);
        assertThat(result.get(0).fournisseurId()).isEqualTo(12L);
        assertThat(result.get(0).statut()).isEqualTo("VALIDEE");
        assertThat(result.get(0).montantTotal()).isEqualByComparingTo(new BigDecimal("450.00"));
    }

    @Test
    void getCommandesByFournisseurId_shouldThrow503WhenDownstreamReturnsServiceUnavailable() {
        mockServer.expect(requestTo("http://service-commandes:8082/api/commandes?fournisseurId=12"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServiceUnavailable());

        assertThatThrownBy(() -> commandesClient.getCommandesByFournisseurId(12L))
                .isInstanceOf(DownstreamServiceUnavailableException.class)
                .hasMessageNotContaining("service-commandes:8082");

        mockServer.verify();
    }

    @Test
    void getCommandesByFournisseurId_shouldThrow503WhenDownstreamTimesOut() {
        mockServer.expect(requestTo("http://service-commandes:8082/api/commandes?fournisseurId=12"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    throw new SocketTimeoutException("Read timed out");
                });

        assertThatThrownBy(() -> commandesClient.getCommandesByFournisseurId(12L))
                .isInstanceOf(DownstreamServiceUnavailableException.class);

        mockServer.verify();
    }

    @Test
    void getCommandesByFournisseurId_shouldThrow502WhenDownstreamReturnsInternalError() {
        mockServer.expect(requestTo("http://service-commandes:8082/api/commandes?fournisseurId=12"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> commandesClient.getCommandesByFournisseurId(12L))
                .isInstanceOf(DownstreamBadGatewayException.class);

        mockServer.verify();
    }

    @Test
    void getCommandesByFournisseurId_shouldReturnEmptyListWhenDownstreamReturns404() {
        mockServer.expect(requestTo("http://service-commandes:8082/api/commandes?fournisseurId=12"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withResourceNotFound());

        List<CommandeSummaryResponse> result = commandesClient.getCommandesByFournisseurId(12L);

        mockServer.verify();
        assertThat(result).isEmpty();
    }

    @Test
    void getCommandesByFournisseurId_shouldThrow502WhenDownstreamReturnsMalformedJson() {
        mockServer.expect(requestTo("http://service-commandes:8082/api/commandes?fournisseurId=12"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("not-valid-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> commandesClient.getCommandesByFournisseurId(12L))
                .isInstanceOf(DownstreamBadGatewayException.class);

        mockServer.verify();
    }
}
