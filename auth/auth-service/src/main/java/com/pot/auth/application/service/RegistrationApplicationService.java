package com.pot.auth.application.service;

import com.pot.auth.application.assembler.AuthCommandAssembler;
import com.pot.auth.application.dto.OneStopAuthResponse;
import com.pot.auth.application.dto.RegisterResponse;
import com.pot.auth.application.strategy.RegisterStrategy;
import com.pot.auth.application.validation.ValidationChain;
import com.pot.auth.application.strategy.factory.RegisterStrategyFactory;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.application.context.RegistrationContext;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.valueobject.DeviceInfo;
import com.pot.auth.domain.shared.valueobject.IpAddress;
import com.pot.auth.interfaces.dto.onestop.OAuth2AuthRequest;
import com.pot.auth.interfaces.dto.onestop.WeChatAuthRequest;
import com.pot.auth.interfaces.dto.register.OAuth2RegisterRequest;
import com.pot.auth.interfaces.dto.register.RegisterRequest;
import com.pot.auth.interfaces.dto.register.WeChatRegisterRequest;
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
        private final AuthCommandAssembler authCommandAssembler;

        /**
         * Executes a register request with the matching flow.
         */
        public RegisterResponse register(RegisterRequest request, String ipAddress, String userAgent) {
                log.info("[应用服务] 注册请求: registerType={}, userDomain={}",
                                request.registerType(), request.userDomain());

                return switch (request.registerType()) {
                        case OAUTH2, WECHAT -> registerThroughOneStop(request, ipAddress, userAgent);
                        default -> registerThroughStrategy(request, ipAddress, userAgent);
                };
        }

        private RegisterResponse registerThroughStrategy(RegisterRequest request, String ipAddress, String userAgent) {
                RegistrationContext context = RegistrationContext.builder()
                                .request(authCommandAssembler.toCommand(request))
                                .ipAddress(IpAddress.of(ipAddress))
                                .deviceInfo(DeviceInfo.fromUserAgent(userAgent != null ? userAgent : "Unknown"))
                                .build();

                registrationValidationChain.validate(context);

                RegisterStrategy strategy = registerStrategyFactory.getStrategy(request.registerType());
                AuthenticationResult result = strategy.execute(context);
                RegisterResponse response = toRegisterResponse(result);

                log.info("[应用服务] 注册成功: userId={}, registerType={}", result.userId(), request.registerType());
                return response;
        }

        private RegisterResponse registerThroughOneStop(RegisterRequest request, String ipAddress, String userAgent) {
                OneStopAuthResponse authResponse = oneStopAuthenticationService.authenticate(
                                toOneStopAuthRequest(request),
                                ipAddress,
                                userAgent);
                return toRegisterResponse(authResponse);
        }

        private com.pot.auth.interfaces.dto.onestop.OneStopAuthRequest toOneStopAuthRequest(RegisterRequest request) {
                return switch (request) {
                        case OAuth2RegisterRequest oauth2Request -> new OAuth2AuthRequest(
                                        AuthType.OAUTH2,
                                        OAuth2AuthRequest.OAuth2Provider.valueOf(
                                                        oauth2Request.provider().getCode().toUpperCase()),
                                        oauth2Request.code(),
                                        oauth2Request.state(),
                                        oauth2Request.userDomain());
                        case WeChatRegisterRequest weChatRequest -> new WeChatAuthRequest(
                                        AuthType.WECHAT,
                                        weChatRequest.code(),
                                        weChatRequest.state(),
                                        weChatRequest.userDomain());
                        default -> throw new IllegalArgumentException("不支持的一键注册类型: " + request.registerType());
                };
        }

        private RegisterResponse toRegisterResponse(AuthenticationResult result) {
                return RegisterResponse.success(
                                result.userId().value(),
                                result.userDomain().name(),
                                result.nickname(),
                                result.email(),
                                result.phone(),
                                result.accessToken(),
                                result.refreshToken(),
                                result.accessTokenExpiresAt(),
                                result.refreshTokenExpiresAt());
        }

        private RegisterResponse toRegisterResponse(OneStopAuthResponse response) {
                return RegisterResponse.success(
                                response.userId().value(),
                                response.userDomain().name(),
                                response.nickname(),
                                response.email(),
                                response.phone(),
                                response.accessToken(),
                                response.refreshToken(),
                                response.accessTokenExpiresAt(),
                                response.refreshTokenExpiresAt());
        }
}
