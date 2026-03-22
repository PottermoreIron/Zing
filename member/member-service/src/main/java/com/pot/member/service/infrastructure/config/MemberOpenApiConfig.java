package com.pot.member.service.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Member 服务 OpenAPI 文档配置
 *
 * <p>
 * 覆盖 framework-common 中的基础 OpenAPI 配置，定制 member-service 专属文档信息。
 *
 * <p>
 * 访问地址：
 * <ul>
 * <li>Swagger UI：<a href=
 * "http://localhost:11000/swagger-ui/index.html">http://localhost:11000/swagger-ui/index.html</a></li>
 * <li>OpenAPI JSON：<a href=
 * "http://localhost:11000/v3/api-docs">http://localhost:11000/v3/api-docs</a></li>
 * </ul>
 *
 * @author Pot
 * @since 2026-03-15
 */
@Configuration
public class MemberOpenApiConfig {

    @Bean
    @Primary
    public OpenAPI memberOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Member Service API")
                        .description("""
                                会员服务接口文档。
                                
                                **接口说明：**
                                提供会员信息查询、资料更新、密码修改等功能。
                                部分接口需要通过请求属性传递 memberId（在正式环境由网关注入）。
                                
                                **快速开始：**
                                1. 直接访问各接口进行测试
                                2. 需要 memberId 的接口，在测试时可通过直接调用方式传参
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("pot")
                                .email("yecao.scu@gmail.com")));
    }
}
