package com.procuraflow.fournisseurs.client;

import com.procuraflow.fournisseurs.dto.CommandeSummaryResponse;
import com.procuraflow.fournisseurs.exception.DownstreamBadGatewayException;
import com.procuraflow.fournisseurs.exception.DownstreamServiceUnavailableException;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * REST client communicating with Yassine's Commandes microservice.
 * Strictly respects schema ownership: never queries schema_commandes directly.
 */
@Component
public class CommandesClient {

    private static final Logger log = LoggerFactory.getLogger(CommandesClient.class);

    private static final ParameterizedTypeReference<List<CommandeSummaryResponse>> COMMANDE_LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient commandesRestClient;

    public CommandesClient(RestClient commandesRestClient) {
        this.commandesRestClient = commandesRestClient;
    }

    public List<CommandeSummaryResponse> getCommandesByFournisseurId(Long fournisseurId) {
        try {
            List<CommandeSummaryResponse> commandes = commandesRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/commandes")
                            .queryParam("fournisseurId", fournisseurId)
                            .build())
                    .retrieve()
                    .body(COMMANDE_LIST_TYPE);

            return commandes != null ? commandes : Collections.emptyList();
        } catch (ResourceAccessException ex) {
            log.warn("Commandes service unreachable or timed out for fournisseurId={}", fournisseurId);
            throw new DownstreamServiceUnavailableException(
                    "Le service Commandes est temporairement indisponible. Veuillez réessayer ultérieurement.",
                    ex
            );
        } catch (HttpServerErrorException ex) {
            log.warn("Commandes service returned HTTP {} for fournisseurId={}", ex.getStatusCode().value(), fournisseurId);
            if (ex.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE
                    || ex.getStatusCode() == HttpStatus.GATEWAY_TIMEOUT) {
                throw new DownstreamServiceUnavailableException(
                        "Le service Commandes est temporairement indisponible.",
                        ex
                );
            }
            throw new DownstreamBadGatewayException(
                    "Le service Commandes a renvoyé une erreur lors de la récupération de l'historique des commandes.",
                    ex
            );
        } catch (HttpClientErrorException ex) {
            log.warn("Commandes service returned HTTP {} for fournisseurId={}", ex.getStatusCode().value(), fournisseurId);
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Collections.emptyList();
            }
            throw new DownstreamBadGatewayException(
                    "Requête d'intégration rejetée par le service Commandes.",
                    ex
            );
        } catch (RestClientException ex) {
            log.warn("Invalid response received from Commandes service for fournisseurId={}", fournisseurId);
            throw new DownstreamBadGatewayException(
                    "Réponse invalide reçue depuis le service Commandes.",
                    ex
            );
        }
    }
}
