package com.pot.zing.framework.starter.authorization.config;

import com.pot.zing.framework.starter.authorization.aspect.RequireAnyPermissionAspect;
import com.pot.zing.framework.starter.authorization.aspect.RequirePermissionAspect;
import com.pot.zing.framework.starter.authorization.aspect.RequireRoleAspect;
import com.pot.zing.framework.starter.authorization.expression.DefaultPermissionExpressionParser;
import com.pot.zing.framework.starter.authorization.expression.PermissionExpressionParser;
import com.pot.zing.framework.starter.authorization.security.AuthorizationSecurityAccessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for authorization components.
 */
@AutoConfiguration
@ConditionalOnClass(AuthorizationSecurityAccessor.class)
@ConditionalOnProperty(prefix = "pot.authorization", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthorizationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PermissionExpressionParser.class)
    public PermissionExpressionParser permissionExpressionParser() {
        return new DefaultPermissionExpressionParser();
    }

    @Bean
    @ConditionalOnBean(AuthorizationSecurityAccessor.class)
    @ConditionalOnMissingBean
    public RequirePermissionAspect requirePermissionAspect(
            PermissionExpressionParser permissionExpressionParser,
            AuthorizationSecurityAccessor securityAccessor) {
        return new RequirePermissionAspect(permissionExpressionParser, securityAccessor);
    }

    @Bean
    @ConditionalOnBean(AuthorizationSecurityAccessor.class)
    @ConditionalOnMissingBean
    public RequireAnyPermissionAspect requireAnyPermissionAspect(
            PermissionExpressionParser permissionExpressionParser,
            AuthorizationSecurityAccessor securityAccessor) {
        return new RequireAnyPermissionAspect(permissionExpressionParser, securityAccessor);
    }

    @Bean
    @ConditionalOnBean(AuthorizationSecurityAccessor.class)
    @ConditionalOnMissingBean
    public RequireRoleAspect requireRoleAspect(AuthorizationSecurityAccessor securityAccessor) {
        return new RequireRoleAspect(securityAccessor);
    }
}