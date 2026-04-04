package com.pot.auth.infrastructure.config;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodePolicy;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.authorization.service.PermissionDomainService;
import com.pot.auth.domain.port.CachePort;
import com.pot.auth.domain.port.DistributedLockPort;
import com.pot.auth.domain.port.NotificationPort;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.TokenManagementPort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import com.pot.auth.application.validation.handler.AuthenticationParameterValidator;
import com.pot.auth.application.validation.handler.OneStopAuthenticationParameterValidator;
import com.pot.auth.application.validation.handler.RegistrationParameterValidator;
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
    public OneStopAuthenticationParameterValidator oneStopAuthenticationParameterValidator() {
        return new OneStopAuthenticationParameterValidator();
    }

    @Bean
    public UserDefaultsGenerator userDefaultsGenerator(AuthDefaultsProperties authDefaultsProperties) {
        AuthDefaultsProperties.PasswordConfig password = authDefaultsProperties.getPassword();
        return new UserDefaultsGenerator(
                authDefaultsProperties.getAvatarUrl(),
                authDefaultsProperties.getUsernamePrefix(),
                password.getLength(),
                password.isIncludeUppercase(),
                password.isIncludeLowercase(),
                password.isIncludeDigits(),
                password.isIncludeSpecial());
    }

    @Bean
    public PermissionDomainService permissionDomainService(
            CachePort cachePort,
            AuthPermissionProperties authPermissionProperties) {
        return new PermissionDomainService(cachePort, authPermissionProperties.getCache().getRedisTtl());
    }

    @Bean
    public JwtTokenService jwtTokenService(
            TokenManagementPort tokenManagementPort,
            CachePort cachePort,
            UserModulePortFactory userModulePortFactory,
            PermissionDomainService permissionDomainService,
            JwtProperties jwtProperties,
            AuthPermissionProperties authPermissionProperties) {
        return new JwtTokenService(
                tokenManagementPort,
                cachePort,
                userModulePortFactory,
                permissionDomainService,
                jwtProperties.getRefreshTokenTtl(),
                jwtProperties.getRefreshTokenSlidingWindow(),
                authPermissionProperties.getCache().isVersionEnabled());
    }

    @Bean
    public VerificationCodeService verificationCodeService(
            CachePort cachePort,
            NotificationPort notificationPort,
            DistributedLockPort distributedLockPort,
            AuthVerificationCodeProperties authVerificationCodeProperties) {
        VerificationCodePolicy policy = new VerificationCodePolicy(
                authVerificationCodeProperties.getCodeKeyPrefix(),
                authVerificationCodeProperties.getAttemptsKeyPrefix(),
                authVerificationCodeProperties.getSendLimitKeyPrefix(),
                authVerificationCodeProperties.getTtlSeconds(),
                authVerificationCodeProperties.getMaxAttempts(),
                authVerificationCodeProperties.getSendCooldownSeconds(),
                authVerificationCodeProperties.getLock().getWaitSeconds(),
                authVerificationCodeProperties.getLock().getLeaseSeconds());
        return new VerificationCodeService(cachePort, notificationPort, distributedLockPort, policy);
    }
}
