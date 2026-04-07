package com.pot.auth.application.service;

import com.pot.auth.application.assembler.AuthCommandAssembler;
import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.application.validation.ValidationChain;
import com.pot.auth.application.strategy.LoginStrategy;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.application.strategy.factory.LoginStrategyFactory;
import com.pot.auth.application.context.AuthenticationContext;
import com.pot.auth.domain.shared.valueobject.DeviceInfo;
import com.pot.auth.domain.shared.valueobject.IpAddress;
import com.pot.auth.interfaces.dto.auth.LoginRequest;
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
    private final AuthCommandAssembler authCommandAssembler;

    /**
     * Executes a login request with the matching strategy.
     */
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        log.info("[登录服务] 登录请求: loginType={}, userDomain={}",
                request.loginType(), request.userDomain());

        AuthenticationContext context = AuthenticationContext.builder()
                .request(authCommandAssembler.toCommand(request))
                .ipAddress(IpAddress.of(ipAddress))
                .deviceInfo(DeviceInfo.fromUserAgent(userAgent != null ? userAgent : "Unknown"))
                .sessionId(generateSessionId())
                .build();

        authenticationValidationChain.validate(context);

        LoginStrategy strategy = loginStrategyFactory.getStrategy(request.loginType());
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

        log.info("[登录服务] 登录成功: userId={}, loginType={}", result.userId(), request.loginType());
        return response;
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }
}
