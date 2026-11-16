package com.arqvora.pix.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI pixOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pix Consulta de Chaves API — Java 21")
                        .version("1.0.0")
                        .description("Laboratorio simulado de consulta/registro de chaves Pix. "
                                + "Nao acessa DICT/SPI/Banco Central e nao realiza pagamentos. "
                                + "JWT compativel com as APIs Saldo :8081 e Extrato :8082."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
