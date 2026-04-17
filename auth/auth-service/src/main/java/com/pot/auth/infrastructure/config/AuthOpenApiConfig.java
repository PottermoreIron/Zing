package com.pot.auth.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * OpenAPI configuration for the auth service.
 */
@Configuration
public class AuthOpenApiConfig {

        private static final String BEARER_SCHEME = "bearerAuth";

        /** Gateway base URL — override at deploy time via GATEWAY_URL env var. */
        @Value("${GATEWAY_URL:http://localhost:8090}")
        private String gatewayUrl;

        @Bean
        @Primary
        public OpenAPI authOpenAPI() {
                return new OpenAPI()
                                // Route all Swagger "Try it out" requests through the gateway.
                                .addServersItem(new Server().url(gatewayUrl).description("Gateway"))
                                .info(new Info()
                                                .title("Auth Service API")
                                                .description("""
                                                                Authentication and authorization service API documentation.

                                                                **Authentication:**
                                                                Login and registration endpoints do not require a token.
                                                                All other endpoints require the header: `Authorization: Bearer {accessToken}`

                                                                **Quick start:**
                                                                1. Call `POST /auth/api/v1/login` to obtain an accessToken
                                                                2. Click `Authorize` (top-right), paste the accessToken into `bearerAuth`
                                                                3. Subsequent requests will automatically carry the Bearer token
                                                                """)
                                                .version("1.0.0")
                                                .contact(new Contact()
                                                                .name("pot")
                                                                .email("yecao.scu@gmail.com")))
                                // Register the Bearer JWT scheme used by secured endpoints.
                                .components(new Components()
                                                .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .scheme("bearer")
                                                                .bearerFormat("JWT")
                                                                .name("Authorization")
                                                                .description("Paste the accessToken from the login response; no Bearer prefix required")))
                                // Public endpoints can still opt out through their own annotations.
                                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
        }
}
