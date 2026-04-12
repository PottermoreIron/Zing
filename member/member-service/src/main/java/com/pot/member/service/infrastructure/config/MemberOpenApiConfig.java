package com.pot.member.service.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MemberOpenApiConfig {

    @Bean
    @Primary
    public OpenAPI memberOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Member Service API")
                        .description("""
                                Member service API documentation.
                                
                                **Overview:**
                                Provides member profiles, profile updates, and password management.
                                Some endpoints require memberId passed via request attributes (injected by the gateway in production).
                                
                                **Quick Start:**
                                1. Call any endpoint directly
                                2. For endpoints requiring memberId, pass it directly during testing
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("pot")
                                .email("yecao.scu@gmail.com")));
    }
}
