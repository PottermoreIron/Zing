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
 * Auth 服务 OpenAPI 文档配置
 *
 * <p>
 * 覆盖 framework-common 中的基础 OpenAPI 配置，增加 JWT Bearer Token 安全方案，
 * 使 Swagger UI 和 Apifox 能够在请求头中自动携带 Authorization: Bearer {token}。
 *
 * <p>
 * 访问地址：
 * <ul>
 * <li>Swagger UI：<a href=
 * "http://localhost:8081/swagger-ui/index.html">http://localhost:8081/swagger-ui/index.html</a></li>
 * <li>OpenAPI JSON：<a href=
 * "http://localhost:8081/v3/api-docs">http://localhost:8081/v3/api-docs</a></li>
 * <li>OpenAPI YAML：<a href=
 * "http://localhost:8081/v3/api-docs.yaml">http://localhost:8081/v3/api-docs.yaml</a></li>
 * </ul>
 *
 * @author pot
 * @since 2026-03-14
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
                // 注册 Bearer JWT 安全方案
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .name("Authorization")
                                .description("粘贴登录接口返回的 accessToken，无需加 Bearer 前缀")))
                // 全局要求 Bearer 认证（公开接口通过 @PublicAccess 注解豁免）
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
