package com.pot.auth.application.service;

import com.pot.auth.application.command.LoginCommand;
import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.application.validation.ValidationChain;
import com.pot.auth.application.strategy.LoginStrategy;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.application.strategy.factory.LoginStrategyFactory;
import com.pot.auth.application.context.AuthenticationContext;
import com.pot.auth.domain.shared.valueobject.DeviceInfo;
import com.pot.auth.domain.shared.valueobject.IpAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Application service for traditional login flows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginApplicationService {

    private final LoginStrategyFactory loginStrategyFactory;
    private final ValidationChain<AuthenticationContext> authenticationValidationChain;

    /**
     * Executes a login command with the matching strategy.
     */
    public LoginResponse login(LoginCommand command, String ipAddress, String userAgent) {
        log.info("[LoginService] Login request — loginType={}, userDomain={}",
                command.loginType(), command.userDomain());

        AuthenticationContext context = AuthenticationContext.builder()
                .request(command)
                .ipAddress(IpAddress.of(ipAddress))
                .deviceInfo(DeviceInfo.fromUserAgent(userAgent != null ? userAgent : "Unknown"))
                .sessionId(generateSessionId())
                .build();

        authenticationValidationChain.validate(context);

        LoginStrategy strategy = loginStrategyFactory.getStrategy(command.loginType());
        AuthenticationResult result = strategy.execute(context);

        LoginResponse response = new LoginResponse(
                result.userId().value(),
                result.userDomain().name(),
                result.nickname(),
                result.email(),
                result.phone(),
                result.accessToken(),
                result.refreshToken(),
                result.accessTokenExpiresAt(),
                result.refreshTokenExpiresAt());

        log.info("[LoginService] Login successful — userId={}, loginType={}", result.userId(), command.loginType());
        return response;
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }
}
