package com.pot.auth.domain.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.valueobject.DeviceInfo;
import com.pot.auth.domain.shared.valueobject.IpAddress;
import com.pot.auth.domain.shared.valueobject.LoginContext;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.interfaces.dto.auth.LoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录策略抽象模板类
 *
 * <p>封装登录流程的通用逻辑，采用模板方法模式
 * <p>通用流程：
 * <ol>
 *   <li>参数验证（由子类实现）</li>
 *   <li>执行登录（由子类实现）</li>
 *   <li>生成Token（模板方法）</li>
 *   <li>记录日志（模板方法）</li>
 * </ol>
 *
 * @author yecao
 * @since 2025-11-18
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractLoginStrategy implements LoginStrategy {

    protected final JwtTokenService jwtTokenService;

    @Override
    public final AuthenticationResult execute(LoginRequest request, String ipAddress, String userAgent) {
        log.info("[登录策略] 开始执行登录: type={}, userDomain={}",
                request.loginType(), request.userDomain());

        try {
            // 1. 构建登录上下文
            LoginContext loginContext = buildLoginContext(ipAddress, userAgent);

            // 2. 参数验证（由子类实现）
            validateRequest(request);

            // 3. 执行登录核心逻辑（由子类实现）
            UserDTO user = doLogin(request, loginContext);

            // 4. 生成认证结果
            AuthenticationResult result = generateAuthenticationResult(
                    user,
                    request.userDomain(),
                    loginContext
            );

            log.info("[登录策略] 登录成功: userId={}, type={}", user.userId(), request.loginType());
            return result;

        } catch (Exception e) {
            log.error("[登录策略] 登录失败: type={}, error={}", request.loginType(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 参数验证（由子类实现）
     *
     * @param request 登录请求
     */
    protected abstract void validateRequest(LoginRequest request);

    /**
     * 执行登录核心逻辑（由子类实现）
     *
     * @param request 登录请求
     * @param loginContext 登录上下文
     * @return 登录后的用户信息
     */
    protected abstract UserDTO doLogin(LoginRequest request, LoginContext loginContext);

    /**
     * 构建登录上下文
     */
    protected LoginContext buildLoginContext(String ipAddress, String userAgent) {
        IpAddress ip = IpAddress.of(ipAddress);
        DeviceInfo deviceInfo = DeviceInfo.fromUserAgent(userAgent != null ? userAgent : "Unknown");
        return LoginContext.of(ip, deviceInfo);
    }

    /**
     * 生成认证结果（包含Token）
     */
    protected AuthenticationResult generateAuthenticationResult(
            UserDTO user,
            UserDomain userDomain,
            LoginContext loginContext
    ) {
        var tokenPair = jwtTokenService.generateTokenPair(
                user.userId(),
                userDomain,
                user.username(),
                user.permissions()
        );

        return AuthenticationResult.builder()
                .userId(user.userId())
                .userDomain(userDomain)
                .username(user.username())
                .email(user.email())
                .phone(user.phone())
                .accessToken(tokenPair.accessToken().rawToken())
                .refreshToken(tokenPair.refreshToken().rawToken())
                .accessTokenExpiresAt(tokenPair.accessToken().expiresAt())
                .refreshTokenExpiresAt(tokenPair.refreshToken().expiresAt())
                .loginContext(loginContext)
                .build();
    }

    /**
     * 获取策略支持的登录类型（由子类实现）
     */
    protected abstract LoginType getSupportedLoginType();

    @Override
    public boolean supports(LoginType loginType) {
        return getSupportedLoginType().equals(loginType);
    }
}
