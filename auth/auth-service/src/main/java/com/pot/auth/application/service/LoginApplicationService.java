package com.pot.auth.application.service;

import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.context.AuthenticationContext;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.strategy.LoginStrategy;
import com.pot.auth.domain.strategy.factory.LoginStrategyFactory;
import com.pot.auth.interfaces.dto.auth.LoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 登录应用服务
 *
 * <p>
 * 负责传统登录流程（要求用户已注册）
 *
 * <p>
 * 支持的登录方式：
 * <ul>
 * <li>用户名 + 密码</li>
 * <li>邮箱 + 密码</li>
 * <li>邮箱 + 验证码</li>
 * <li>手机号 + 验证码</li>
 * </ul>
 *
 * <p>
 * 注意：
 * <ul>
 * <li>如需一键认证（自动注册/登录，包括 OAuth2/WeChat），请使用
 * {@link OneStopAuthenticationService}</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginApplicationService {

    private final LoginStrategyFactory loginStrategyFactory;

    /**
     * 传统登录入口
     *
     * <p>
     * 根据登录类型自动选择对应的策略执行登录
     *
     * <p>
     * 不支持第三方登录（OAuth2/WeChat），这些已迁移到 AuthenticationController
     *
     * @param request   登录请求（多态）
     * @param ipAddress 客户端IP地址
     * @param userAgent 用户代理信息
     * @return 登录响应
     * @throws DomainException 如果使用不支持的登录类型
     */
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        log.info("[登录服务] 登录请求: loginType={}, userDomain={}",
                request.loginType(), request.userDomain());

        // 传统登录（用户名/邮箱/手机号 + 密码/验证码）
        // 构建认证上下文
        AuthenticationContext context = AuthenticationContext.builder()
                .request(request)
                .ipAddress(com.pot.auth.domain.shared.valueobject.IpAddress.of(ipAddress))
                .deviceInfo(com.pot.auth.domain.shared.valueobject.DeviceInfo
                        .fromUserAgent(userAgent != null ? userAgent : "Unknown"))
                .sessionId(generateSessionId())
                .build();

        // 获取策略并执行
        LoginStrategy<?> strategy = loginStrategyFactory.getStrategy(request.loginType());
        // 此处的类型转换是安全的，因为工厂根据loginType返回对应的策略
        AuthenticationResult result = strategy.execute(context);

        // 转换为应用层DTO
        LoginResponse response = new LoginResponse(
                result.userId().value(),
                result.userDomain().name(),
                result.username(),
                result.email(),
                result.phone(),
                result.accessToken(),
                result.refreshToken(),
                result.accessTokenExpiresAt(),
                result.refreshTokenExpiresAt());

        log.info("[登录服务] 登录成功: userId={}, loginType={}", result.userId(), request.loginType());
        return response;
    }

    /**
     * 生成会话ID
     *
     * @return 会话ID
     */
    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }
}
