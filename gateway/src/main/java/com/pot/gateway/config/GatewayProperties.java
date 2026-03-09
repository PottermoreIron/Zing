package com.pot.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 网关全局配置属性
 *
 * <p>
 * 支持通过 application.yml 灵活配置白名单等参数，避免硬编码。
 *
 * @author pot
 * @since 2026-03-09
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.security")
public class GatewayProperties {

    /**
     * 鉴权白名单路径（支持前缀匹配）
     * <p>
     * 默认放行 auth 服务的公开端点
     */
    private List<String> whiteList = List.of(
            "/auth/api/v1/login",
            "/auth/api/v1/register",
            "/auth/api/v1/refresh",
            "/auth/api/v1/authenticate",
            "/auth/code/email",
            "/auth/code/sms",
            "/actuator/health");

    /**
     * 内部服务路径前缀（直接 403，不允许外部访问）
     */
    private List<String> internalPathPrefixes = List.of(
            "/internal/",
            "/member/internal/");
}
