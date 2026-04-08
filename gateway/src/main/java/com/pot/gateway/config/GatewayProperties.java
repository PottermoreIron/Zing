package com.pot.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Security-related gateway properties loaded from configuration.
 *
 * @author pot
 * @since 2026-03-09
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gateway.security")
public class GatewayProperties {

        /** Routes that bypass gateway token validation through prefix matching. */
        private List<String> whiteList = List.of(
                        "/auth/api/v1/login",
                        "/auth/api/v1/register",
                        "/auth/api/v1/refresh",
                        "/auth/api/v1/authenticate",
                        "/auth/code/email",
                        "/auth/code/sms",
                        "/actuator/health");

        /**
         * Routes that are reserved for internal traffic and must return 403 externally.
         */
        private List<String> internalPathPrefixes = List.of(
                        "/internal/",
                        "/member/internal/");
}
