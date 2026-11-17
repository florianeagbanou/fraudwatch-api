package com.fraudwatch.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Swagger UI metadata, plus an "Authorize" button that accepts a JWT. */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "FraudWatch API",
                version = "1.0.0",
                description = "Banking API with real-time, rule-based fraud detection. "
                        + "Log in with POST /api/auth/login, then click Authorize and paste the token."),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
