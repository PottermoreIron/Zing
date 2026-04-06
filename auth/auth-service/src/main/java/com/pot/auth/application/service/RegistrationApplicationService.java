package com.pot.auth.application.service;

import com.pot.auth.application.dto.OneStopAuthResponse;
import com.pot.auth.application.dto.RegisterResponse;
import com.pot.auth.application.command.RegisterCommand;
import com.pot.auth.application.strategy.RegisterStrategy;
import com.pot.auth.application.validation.ValidationChain;
import com.pot.auth.application.strategy.factory.RegisterStrategyFactory;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.application.context.RegistrationContext;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.valueobject.DeviceInfo;
import com.pot.auth.domain.shared.valueobject.IpAddress;
import com.pot.auth.interfaces.dto.onestop.OAuth2AuthRequest;
import com.pot.auth.interfaces.dto.onestop.WeChatAuthRequest;
import com.pot.auth.interfaces.dto.register.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application service for registration flows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationApplicationService {

        private final RegisterStrategyFactory registerStrategyFactory;
        private final ValidationChain<RegistrationContext> registrationValidationChain;
        private final OneStopAuthenticationService oneStopAuthenticationService;

        /**
         * Executes a register request with the matching flow.
         */
        public RegisterResponse register(RegisterRequest request, String ipAddress, String userAgent) {
                log.info("[应用服务] 注册请求: registerType={}, userDomain={}",
                                request.registerType(), request.userDomain());

                AuthenticationResult result;

                if (RegisterType.OAUTH2.equals(request.registerType())) {
                        OAuth2AuthRequest authRequest = new OAuth2AuthRequest(
                                        AuthType.OAUTH2,
                                        OAuth2AuthRequest.OAuth2Provider
                                                        .valueOf(request.oauth2ProviderCode().toUpperCase()),
                                        request.code(),
                                        request.state(),
                                        request.userDomain());

                        OneStopAuthResponse authResponse = oneStopAuthenticationService.authenticate(
                                        authRequest, ipAddress, userAgent);

                        return RegisterResponse.success(
                                        authResponse.userId().value(),
                                        authResponse.userDomain().name(),
                                        authResponse.nickname(),
                                        authResponse.email(),
                                        authResponse.phone(),
                                        authResponse.accessToken(),
                                        authResponse.refreshToken(),
                                        authResponse.accessTokenExpiresAt(),
                                        authResponse.refreshTokenExpiresAt());

                } else if (RegisterType.WECHAT.equals(request.registerType())) {
                        WeChatAuthRequest authRequest = new WeChatAuthRequest(
                                        AuthType.WECHAT,
                                        request.code(),
                                        request.state(),
                                        request.userDomain());

                        OneStopAuthResponse authResponse = oneStopAuthenticationService.authenticate(
                                        authRequest, ipAddress, userAgent);

                        return RegisterResponse.success(
                                        authResponse.userId().value(),
                                        authResponse.userDomain().name(),
                                        authResponse.nickname(),
                                        authResponse.email(),
                                        authResponse.phone(),
                                        authResponse.accessToken(),
                                        authResponse.refreshToken(),
                                        authResponse.accessTokenExpiresAt(),
                                        authResponse.refreshTokenExpiresAt());

                } else {
                        RegistrationContext context = RegistrationContext.builder()
                                        .request(toCommand(request))
                                        .ipAddress(IpAddress.of(ipAddress))
                                        .deviceInfo(DeviceInfo.fromUserAgent(userAgent != null ? userAgent : "Unknown"))
                                        .build();

                        registrationValidationChain.validate(context);

                        RegisterStrategy strategy = registerStrategyFactory.getStrategy(request.registerType());
                        result = strategy.execute(context);

                        RegisterResponse response = RegisterResponse.success(
                                        result.userId().value(),
                                        result.userDomain().name(),
                                        result.nickname(),
                                        result.email(),
                                        result.phone(),
                                        result.accessToken(),
                                        result.refreshToken(),
                                        result.accessTokenExpiresAt(),
                                        result.refreshTokenExpiresAt());

                        log.info("[应用服务] 注册成功: userId={}, registerType={}", result.userId(), request.registerType());
                        return response;
                }
        }

        private RegisterCommand toCommand(RegisterRequest request) {
                return new RegisterCommand() {
                        @Override
                        public RegisterType registerType() {
                                return request.registerType();
                        }

                        @Override
                        public com.pot.auth.domain.shared.valueobject.UserDomain userDomain() {
                                return request.userDomain();
                        }

                        @Override
                        public String nickname() {
                                return request.nickname();
                        }

                        @Override
                        public String email() {
                                return request.email();
                        }

                        @Override
                        public String phone() {
                                return request.phone();
                        }

                        @Override
                        public String password() {
                                return request.password();
                        }

                        @Override
                        public String verificationCode() {
                                return request.verificationCode();
                        }

                        @Override
                        public String code() {
                                return request.code();
                        }

                        @Override
                        public String state() {
                                return request.state();
                        }

                        @Override
                        public String oauth2ProviderCode() {
                                return request.oauth2ProviderCode();
                        }
                };
        }
}
