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
