package com.pot.auth.infrastructure.config;

import com.pot.auth.application.context.AuthenticationContext;
import com.pot.auth.application.context.OneStopAuthContext;
import com.pot.auth.application.context.RegistrationContext;
import com.pot.auth.application.validation.ValidationChain;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
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
import com.pot.auth.application.validation.handler.OneStopAuthenticationParameterValidator;
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

    @Mock
    private AuthenticationParameterValidator authenticationParameterValidator;

    @Mock
    private RegistrationParameterValidator registrationParameterValidator;

    @Mock
    private OneStopAuthenticationParameterValidator oneStopAuthenticationParameterValidator;

    private final DomainServiceConfig config = new DomainServiceConfig();
    private final ApplicationValidationConfig applicationValidationConfig = new ApplicationValidationConfig();

    @Nested
    @DisplayName("domain bean assembly")
    class DomainBeanAssembly {

        @Test
        @DisplayName("should create application validation chains through configuration")
        void shouldCreateApplicationValidationChainsThroughConfiguration() {
            ValidationChain<AuthenticationContext> authenticationValidationChain = applicationValidationConfig
                .authenticationValidationChain(authenticationParameterValidator);
            ValidationChain<RegistrationContext> registrationValidationChain = applicationValidationConfig
                .registrationValidationChain(registrationParameterValidator);
            ValidationChain<OneStopAuthContext> oneStopValidationChain = applicationValidationConfig
                .oneStopAuthValidationChain(oneStopAuthenticationParameterValidator);

            AuthDefaultsProperties authDefaultsProperties = new AuthDefaultsProperties();
            UserDefaultsGenerator userDefaultsGenerator = config.userDefaultsGenerator(authDefaultsProperties);

            assertThat(authenticationValidationChain).isNotNull();
            assertThat(registrationValidationChain).isNotNull();
            assertThat(oneStopValidationChain).isNotNull();
            assertThat(userDefaultsGenerator).isNotNull();
        }

        @Test
        @DisplayName("should assemble domain services with required dependencies")
        void shouldAssembleDomainServicesWithRequiredDependencies() {
            JwtProperties jwtProperties = new JwtProperties();
            jwtProperties.setRefreshTokenTtl(7200L);
            jwtProperties.setRefreshTokenSlidingWindow(1800L);

            AuthPermissionProperties authPermissionProperties = new AuthPermissionProperties();
            authPermissionProperties.getCache().setRedisTtl(3600L);
            authPermissionProperties.getCache().setVersionEnabled(true);

            AuthVerificationCodeProperties verificationCodeProperties = new AuthVerificationCodeProperties();

            PermissionDomainService permissionDomainService = config.permissionDomainService(cachePort,
                    authPermissionProperties);
            UserModulePortFactory userModulePortFactory = config.userModulePortFactory(List.of(userModulePort));
            JwtTokenService jwtTokenService = config.jwtTokenService(
                    tokenManagementPort,
                    cachePort,
                    userModulePortFactory,
                    permissionDomainService,
                    jwtProperties,
                    authPermissionProperties);
            VerificationCodeService verificationCodeService = config.verificationCodeService(
                    cachePort,
                    notificationPort,
                    distributedLockPort,
                    verificationCodeProperties);

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