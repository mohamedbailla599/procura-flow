package com.procuraflow.fournisseurs;

import com.procuraflow.fournisseurs.config.CommandesClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CommandesClientProperties.class)
public class FournisseursApplication {

    public static void main(String[] args) {
        SpringApplication.run(FournisseursApplication.class, args);
    }
}
