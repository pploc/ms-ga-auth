package com.gymapi.auth.adapter.in.web.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the live API served at {@code /v3/api-docs} and {@code /swagger-ui.html}.
 *
 * <p>Schemas and per-operation detail come from the DTOs and controller annotations, which are
 * generated from and kept aligned with {@code api/openapi/auth-api.yaml}. Only the document-level
 * metadata is set here — keep it in step with the {@code info} block of that file.
 */
@Configuration
public class OpenApiConfig {

  static final String SECURITY_SCHEME_NAME = "bearerAuth";

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("ms-ga-auth API")
                .version("1.0.0")
                .description(
                    """
                    Authorization management for the GymAPI platform — the service that answers \
                    "what can this user do?". Identity itself (register, login, token issuance) \
                    is owned by ms-ga-identifier.

                    Every non-2xx response uses the same `ErrorResponse` envelope. Branch on the \
                    stable `code` field rather than on `message` or on the HTTP status alone, and \
                    quote the `traceId` (also returned as the `X-Correlation-Id` header) when \
                    reporting a problem.""")
                .contact(new Contact().name("GymAPI Platform Team").email("platform@gymapi.local")))
        .servers(
            List.of(
                new Server().url("http://localhost:8082").description("Local development (direct)"),
                new Server().url("http://localhost:8000").description("Kong API gateway")))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
        .components(
            new Components()
                .addSecuritySchemes(
                    SECURITY_SCHEME_NAME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "JWT issued by ms-ga-identifier, carrying the caller's roles and"
                                + " permissions as claims.")));
  }
}
