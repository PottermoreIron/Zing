package com.pot.auth.infrastructure.config;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.authorization.expression.PermissionExpressionParser;
import com.pot.auth.domain.authorization.service.PermissionDomainService;
import com.pot.auth.domain.port.CachePort;
import com.pot.auth.domain.port.DistributedLockPort;
import com.pot.auth.domain.port.NotificationPort;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.TokenManagementPort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import com.pot.auth.application.validation.handler.AuthenticationParameterValidator;
import com.pot.auth.application.validation.handler.RegistrationParameterValidator;
import com.pot.auth.infrastructure.expression.DefaultPermissionExpressionParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 领域服务 Spring Bean 装配配置
 *
 * <p>
 * 领域层的服务类不包含任何 Spring 注解，由此处统一装配成 Bean，
 * 并将基础设施配置值注入到领域服务中。
 *
 * @author pot
 * @since 2026-03-22
 */
@Configuration
public class DomainServiceConfig {

    @Bean
    public UserModulePortFactory userModulePortFactory(List<UserModulePort> userModulePorts) {
        return new UserModulePortFactory(userModulePorts);
    }

    @Bean
    public AuthenticationParameterValidator authenticationParameterValidator() {
        return new AuthenticationParameterValidator();
    }

    @Bean
    public RegistrationParameterValidator registrationParameterValidator() {
        return new RegistrationParameterValidator();
    }

    @Bean
    public PermissionExpressionParser permissionExpressionParser() {
        return new DefaultPermissionExpressionParser();
    }

    @Bean
    public UserDefaultsGenerator userDefaultsGenerator() {
        return new UserDefaultsGenerator();
    }

    @Bean
    public PermissionDomainService permissionDomainService(
            CachePort cachePort,
            @Value("${auth.permission.cache.ttl:3600}") long permissionCacheTtl) {
        return new PermissionDomainService(cachePort, permissionCacheTtl);
    }

    @Bean
    public JwtTokenService jwtTokenService(
            TokenManagementPort tokenManagementPort,
            CachePort cachePort,
            UserModulePortFactory userModulePortFactory,
            PermissionDomainService permissionDomainService,
            JwtProperties jwtProperties,
            @Value("${auth.permission.cache.version-enabled:true}") boolean permissionVersionEnabled) {
        return new JwtTokenService(
                tokenManagementPort,
                cachePort,
                userModulePortFactory,
                permissionDomainService,
                jwtProperties.getRefreshTokenTtl(),
                jwtProperties.getRefreshTokenSlidingWindow(),
                permissionVersionEnabled);
    }

    @Bean
    public VerificationCodeService verificationCodeService(
            CachePort cachePort,
            NotificationPort notificationPort,
            DistributedLockPort distributedLockPort) {
        return new VerificationCodeService(cachePort, notificationPort, distributedLockPort);
    }
}
