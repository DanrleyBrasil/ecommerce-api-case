package com.danrley.ecommerce.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Minha API", version = "v1", description = "Documentação da Minha API"),
        // Aplica o requisito de segurança globalmente a todos os endpoints
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth", // Um nome para a sua definição de segurança
        description = "Token JWT para autenticação. Insira 'Bearer ' antes do seu token.",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP, // O tipo de esquema é HTTP
        bearerFormat = "JWT", // O formato do token
        in = SecuritySchemeIn.HEADER, // O token é enviado no header
        paramName = "Authorization" // O nome do header é "Authorization"
)
public class OpenApiConfig {
    // Esta classe pode ficar vazia, as anotações fazem todo o trabalho.
}