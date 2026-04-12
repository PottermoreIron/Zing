package com.pot.auth.application.service;

import com.pot.auth.application.command.OneStopAuthCommand;
import com.pot.auth.application.command.OneStopAuthRequestCommand;
import com.pot.auth.application.command.RegisterCommand;
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
         * Executes a register command with the matching flow.
         */
        public RegisterResponse register(RegisterCommand command, String ipAddress, String userAgent) {
                log.info("[AppService] Registration request — registerType={}, userDomain={}",
                                command.registerType(), command.userDomain());

                return switch (command.registerType()) {
                        case OAUTH2, WECHAT -> registerThroughOneStop(command, ipAddress, userAgent);
                        default -> registerThroughStrategy(command, ipAddress, userAgent);
                };
        }

        private RegisterResponse registerThroughStrategy(RegisterCommand command, String ipAddress, String userAgent) {
                RegistrationContext context = RegistrationContext.builder()
                                .request(command)
                                .ipAddress(IpAddress.of(ipAddress))
                                .deviceInfo(DeviceInfo.fromUserAgent(userAgent != null ? userAgent : "Unknown"))
                                .build();

                registrationValidationChain.validate(context);

                RegisterStrategy strategy = registerStrategyFactory.getStrategy(command.registerType());
                AuthenticationResult result = strategy.execute(context);
                RegisterResponse response = toRegisterResponse(result);

                log.info("[AppService] Registration successful — userId={}, registerType={}", result.userId(),
                                command.registerType());
                return response;
        }

        private RegisterResponse registerThroughOneStop(RegisterCommand command, String ipAddress, String userAgent) {
                OneStopAuthResponse authResponse = oneStopAuthenticationService.authenticate(
                                toOneStopAuthCommand(command),
                                ipAddress,
                                userAgent);
                return toRegisterResponse(authResponse);
        }

        private OneStopAuthCommand toOneStopAuthCommand(RegisterCommand command) {
                return new OneStopAuthRequestCommand(
                                AuthType.fromCode(command.registerType().getCode()),
                                command.userDomain(),
                                command.nickname(),
                                command.email(),
                                command.phone(),
                                command.password(),
                                command.verificationCode(),
                                command.code(),
                                command.state(),
                                command.oauth2ProviderCode());
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
