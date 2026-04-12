package com.pot.zing.framework.common.config;

import com.pot.zing.framework.common.handler.BaseGlobalExceptionHandler;
import com.pot.zing.framework.common.handler.DefaultGlobalExceptionHandler;
import com.pot.zing.framework.common.properties.JwtProperties;
import com.pot.zing.framework.common.util.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for shared framework utilities.
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
public class CommonAutoConfiguration {

    public CommonAutoConfiguration() {
        log.info("Loading common auto-configuration");
    }

    /**
     * Registers JwtUtils when JWT support is enabled.
     */
    @Bean
    @ConditionalOnClass(name = { "io.jsonwebtoken.Jwts", "jakarta.servlet.http.HttpServletRequest" })
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "pot.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JwtUtils jwtUtils(JwtProperties jwtProperties) {
        log.info("Initializing JwtUtils — issuer: {}", jwtProperties.getIssuer());
        return new JwtUtils(jwtProperties);
    }

    @Bean
    @ConditionalOnClass(name = "jakarta.servlet.ServletException")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(BaseGlobalExceptionHandler.class)
    public DefaultGlobalExceptionHandler defaultGlobalExceptionHandler() {
        log.info("Initializing default global exception handler");
        return new DefaultGlobalExceptionHandler();
    }
}
