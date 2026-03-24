package com.pot.auth.infrastructure.config;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.authorization.expression.PermissionExpressionParser;
import com.pot.auth.domain.authorization.service.PermissionDomainService;
import com.pot.auth.domain.port.CachePort;
import com.pot.auth.domain.port.DistributedLockPort;
import com.pot.auth.domain.port.NotificationPort;
import com.pot.auth.domain.port.TokenManagementPort;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.application.validation.handler.AuthenticationParameterValidator;
import com.pot.auth.application.validation.handler.RegistrationParameterValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DomainServiceConfig")
class DomainServiceConfigTest {

    @Mock
    private CachePort cachePort;

    @Mock
    private DistributedLockPort distributedLockPort;

    @Mock
    private NotificationPort notificationPort;

    @Mock
    private TokenManagementPort tokenManagementPort;

    @Mock
    private UserModulePort userModulePort;

    private final DomainServiceConfig config = new DomainServiceConfig();

    @Nested
    @DisplayName("domain bean assembly")
    class DomainBeanAssembly {

        @Test
        @DisplayName("should create stateless domain helpers through configuration")
        void shouldCreateStatelessDomainHelpersThroughConfiguration() {
            AuthenticationParameterValidator authenticationValidator = config.authenticationParameterValidator();
            RegistrationParameterValidator registrationValidator = config.registrationParameterValidator();
            PermissionExpressionParser permissionExpressionParser = config.permissionExpressionParser();
            UserDefaultsGenerator userDefaultsGenerator = config.userDefaultsGenerator();

            assertThat(authenticationValidator).isNotNull();
            assertThat(registrationValidator).isNotNull();
            assertThat(permissionExpressionParser).isNotNull();
            assertThat(userDefaultsGenerator).isNotNull();
        }

        @Test
        @DisplayName("should assemble domain services with required dependencies")
        void shouldAssembleDomainServicesWithRequiredDependencies() {
            JwtProperties jwtProperties = new JwtProperties();
            jwtProperties.setRefreshTokenTtl(7200L);
            jwtProperties.setRefreshTokenSlidingWindow(1800L);

            PermissionDomainService permissionDomainService = config.permissionDomainService(cachePort, 3600L);
            UserModulePortFactory userModulePortFactory = config.userModulePortFactory(List.of(userModulePort));
            JwtTokenService jwtTokenService = config.jwtTokenService(
                    tokenManagementPort,
                    cachePort,
                    userModulePortFactory,
                    permissionDomainService,
                    jwtProperties,
                    true);
            VerificationCodeService verificationCodeService = config.verificationCodeService(
                    cachePort,
                    notificationPort,
                    distributedLockPort);

            assertThat(permissionDomainService).isNotNull();
            assertThat(jwtTokenService).isNotNull();
            assertThat(verificationCodeService).isNotNull();
        }

        @Test
        @DisplayName("should build user module port factory from adapter list")
        void shouldBuildUserModulePortFactoryFromAdapterList() {
            when(userModulePort.supportedDomain()).thenReturn(UserDomain.MEMBER);

            UserModulePortFactory factory = config.userModulePortFactory(List.of(userModulePort));

            assertThat(factory.supports(UserDomain.MEMBER)).isTrue();
            assertThat(factory.getPort(UserDomain.MEMBER)).isSameAs(userModulePort);
            assertThat(factory.getSupportedDomains()).containsExactly(UserDomain.MEMBER);
        }
    }
}