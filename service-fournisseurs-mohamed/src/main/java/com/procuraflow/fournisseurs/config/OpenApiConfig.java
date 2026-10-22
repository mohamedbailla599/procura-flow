package com.procuraflow.fournisseurs.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fournisseursOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ProcuraFlow — API Service Fournisseurs")
                        .version("1.0.0")
                        .description("Microservice de gestion des fournisseurs (CRUD, désactivation logique et historique des commandes via le service Commandes).")
                        .contact(new Contact()
                                .name("Mohamed Bailla — Équipe ProcuraFlow")));
    }
}
