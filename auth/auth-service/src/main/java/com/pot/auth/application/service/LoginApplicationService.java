package com.pot.auth.application.service;

import com.pot.auth.application.command.LoginCommand;
import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.authentication.service.AuthenticationDomainService;
import com.pot.auth.domain.shared.valueobject.DeviceInfo;
import com.pot.auth.domain.shared.valueobject.IpAddress;
import com.pot.auth.domain.shared.valueobject.LoginContext;
import com.pot.auth.domain.shared.valueobject.Password;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 登录应用服务
 *
 * <p>编排登录流程，协调领域服务完成业务逻辑
 *
 * @author yecao
 * @since 2025-11-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginApplicationService {

    private final AuthenticationDomainService authenticationDomainService;

    /**
     * 密码登录
     *
     * @param command 登录命令
     * @return 登录响应
     */
    public LoginResponse loginWithPassword(LoginCommand command) {
        log.info("[应用服务] 密码登录: identifier={}, userDomain={}",
                command.identifier(), command.userDomain());

        // 1. 构建值对象
        Password password = Password.of(command.password());
        IpAddress ipAddress = IpAddress.of(command.ipAddress());
        DeviceInfo deviceInfo = command.userAgent() != null
                ? DeviceInfo.fromUserAgent(command.userAgent())
                : DeviceInfo.fromUserAgent("Unknown");
        LoginContext loginContext = LoginContext.builder()
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .build();

        // 2. 调用领域服务进行认证
        AuthenticationResult result = authenticationDomainService.authenticateWithPassword(
                command.identifier(),
                password,
                command.userDomain(),
                loginContext
        );

        // 3. 转换为应用层DTO
        LoginResponse response = new LoginResponse(
                result.userId().value(),
                result.userDomain().name(),
                result.username(),
                result.email(),
                result.phone(),
                result.accessToken(),
                result.refreshToken(),
                result.accessTokenExpiresAt(),
                result.refreshTokenExpiresAt()
        );

        log.info("[应用服务] 密码登录成功: userId={}", result.userId());
        return response;
    }
}

