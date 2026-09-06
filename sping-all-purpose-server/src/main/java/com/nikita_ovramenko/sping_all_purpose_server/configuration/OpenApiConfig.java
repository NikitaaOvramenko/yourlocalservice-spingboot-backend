package com.nikita_ovramenko.sping_all_purpose_server.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenAPI document metadata and the bearer-token scheme.
 *
 * <p>The security scheme is what puts the "Authorize" button in Swagger UI. Without it
 * the UI has nowhere to hold a JWT, so every /api/admin request from the try-it-out
 * panel comes back 401 and the docs are read-only.
 *
 * <p>Whether any of this is exposed is controlled by SWAGGER_ENABLED (see
 * application.properties); when disabled, springdoc registers no routes at all.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI apiDocumentation() {
        return new OpenAPI()
                .info(new Info()
                        .title("YourLocalService backend")
                        .version("v1")
                        .description("""
                                Public quote funnel plus an admin API over quotes, jobs, \
                                users and organizations.

                                Everything under /api/admin requires a bearer token from \
                                POST /api/auth/login belonging to an account with the ADMIN role.

                                PATCH semantics throughout: a field omitted or sent as null \
                                is left unchanged. Nullable fields cannot be cleared via PATCH."""))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the accessToken from POST /api/auth/login.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
