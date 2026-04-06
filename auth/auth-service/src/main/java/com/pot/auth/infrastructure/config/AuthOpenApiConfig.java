package com.pot.auth.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * OpenAPI configuration for the auth service.
 */
@Configuration
public class AuthOpenApiConfig {

        private static final String BEARER_SCHEME = "bearerAuth";

        @Bean
        @Primary
        public OpenAPI authOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Auth Service API")
                                                .description("""
                                                                认证授权服务接口文档。

                                                                **鉴权说明：**
                                                                登录/注册接口无需 Token。
                                                                其他接口需在 Header 中携带：`Authorization: Bearer {accessToken}`

                                                                **快速开始：**
                                                                1. 调用 `POST /auth/api/v1/login` 获取 accessToken
                                                                2. 点击右上角 `Authorize`，在 `bearerAuth` 栏粘贴 accessToken
                                                                3. 后续接口请求将自动携带 Bearer Token
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
                                                                .description("粘贴登录接口返回的 accessToken，无需加 Bearer 前缀")))
                                // Public endpoints can still opt out through their own annotations.
                                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
        }
}
