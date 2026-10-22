package com.procuraflow.fournisseurs.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public RestClientCustomizer commandesTimeoutRestClientCustomizer(CommandesClientProperties properties) {
        return restClientBuilder -> {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
            requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
            restClientBuilder
                    .baseUrl(properties.getBaseUrl())
                    .requestFactory(requestFactory)
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        };
    }

    @Bean
    public RestClient commandesRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
