package com.pot.zing.framework.starter.security.config;

import com.pot.zing.framework.starter.security.adapter.SpringSecurityContextAdapter;
import com.pot.zing.framework.starter.security.aspect.RequireAnyPermissionAspect;
import com.pot.zing.framework.starter.security.aspect.RequirePermissionAspect;
import com.pot.zing.framework.starter.security.aspect.RequireRoleAspect;
import com.pot.zing.framework.starter.security.expression.DefaultPermissionExpressionParser;
import com.pot.zing.framework.starter.security.expression.PermissionExpressionParser;
import com.pot.zing.framework.starter.security.filter.GatewayHeaderAuthenticationFilter;
import com.pot.zing.framework.starter.security.port.SecurityContextPort;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the security anti-corruption layer.
 *
 * <p>Registers:
 * <ul>
 *   <li>{@link SpringSecurityContextAdapter} as the default {@link SecurityContextPort}</li>
 *   <li>{@link DefaultPermissionExpressionParser} as the default expression parser</li>
 *   <li>Authorization aspects wired against {@link SecurityContextPort}</li>
 *   <li>{@link GatewayHeaderAuthenticationFilter} bean (servlet auto-registration disabled;
 *       each downstream service adds it to its own {@code SecurityFilterChain})</li>
 * </ul>
 * </p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "pot.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecurityContextPort.class)
    public SecurityContextPort springSecurityContextAdapter() {
        return new SpringSecurityContextAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(PermissionExpressionParser.class)
    public PermissionExpressionParser permissionExpressionParser() {
        return new DefaultPermissionExpressionParser();
    }

    @Bean
    @ConditionalOnMissingBean(RequirePermissionAspect.class)
    public RequirePermissionAspect requirePermissionAspect(
            PermissionExpressionParser parser, SecurityContextPort port) {
        return new RequirePermissionAspect(parser, port);
    }

    @Bean
    @ConditionalOnMissingBean(RequireAnyPermissionAspect.class)
    public RequireAnyPermissionAspect requireAnyPermissionAspect(
            PermissionExpressionParser parser, SecurityContextPort port) {
        return new RequireAnyPermissionAspect(parser, port);
    }

    @Bean
    @ConditionalOnMissingBean(RequireRoleAspect.class)
    public RequireRoleAspect requireRoleAspect(SecurityContextPort port) {
        return new RequireRoleAspect(port);
    }

    // The filter is provided as a bean so downstream services can inject it into their
    // SecurityFilterChain, but auto-registration at the servlet level is disabled to
    // avoid double-invocation.
    @Bean
    @ConditionalOnMissingBean(GatewayHeaderAuthenticationFilter.class)
    public GatewayHeaderAuthenticationFilter gatewayHeaderAuthenticationFilter() {
        return new GatewayHeaderAuthenticationFilter();
    }

    @Bean
    public FilterRegistrationBean<GatewayHeaderAuthenticationFilter> gatewayHeaderFilterRegistration(
            GatewayHeaderAuthenticationFilter filter) {
        FilterRegistrationBean<GatewayHeaderAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        // Prevent Spring Boot from auto-registering this filter at the servlet level;
        // it is exclusively managed by each service's SecurityFilterChain.
        registration.setEnabled(false);
        return registration;
    }
}
