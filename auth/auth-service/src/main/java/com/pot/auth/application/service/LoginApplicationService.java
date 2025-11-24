package com.pot.auth.application.service;

import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.strategy.AuthenticationStrategy;
import com.pot.auth.domain.strategy.LoginStrategy;
import com.pot.auth.domain.strategy.factory.AuthenticationStrategyFactory;
import com.pot.auth.domain.strategy.factory.LoginStrategyFactory;
import com.pot.auth.interfaces.dto.auth.LoginRequest;
import com.pot.auth.interfaces.dto.auth.OAuth2LoginRequest;
import com.pot.auth.interfaces.dto.auth.WeChatLoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 登录应用服务
 *
 * <p>编排登录流程，根据登录类型自动选择对应的策略
 * <p>支持7种登录方式：
 * <ul>
 *   <li>用户名密码登录</li>
 *   <li>手机号密码登录</li>
 *   <li>邮箱密码登录</li>
 *   <li>手机号验证码登录</li>
 *   <li>邮箱验证码登录</li>
 *   <li>OAuth2登录（Google, GitHub等）</li>
 *   <li>微信登录</li>
 * </ul>
 *
 * @author yecao
 * @since 2025-11-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginApplicationService {

    private final LoginStrategyFactory loginStrategyFactory;
    private final AuthenticationStrategyFactory authenticationStrategyFactory;

    /**
     * 统一登录入口
     *
     * <p>根据登录类型自动选择对应的策略执行登录
     *
     * @param request 登录请求（多态）
     * @param ipAddress 客户端IP地址
     * @param userAgent 用户代理信息
     * @return 登录响应
     */
    @SuppressWarnings("unchecked")
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        log.info("[应用服务] 登录请求: loginType={}, userDomain={}",
                request.loginType(), request.userDomain());

        AuthenticationResult result;

        // 判断是传统登录还是一体化认证（OAuth2/WeChat）
        if (LoginType.OAUTH2.equals(request.loginType())) {
            // OAuth2登录
            OAuth2LoginRequest oauthReq = (OAuth2LoginRequest) request;
            AuthenticationStrategy strategy = authenticationStrategyFactory.getStrategy(LoginType.OAUTH2);
            result = strategy.authenticate(
                    oauthReq.provider().getCode(),
                    oauthReq.code(),
                    oauthReq.state(),
                    oauthReq.userDomain().getCode(),
                    ipAddress,
                    userAgent
            );
        } else if (LoginType.WECHAT.equals(request.loginType())) {
            // 微信登录
            WeChatLoginRequest wechatReq = (WeChatLoginRequest) request;
            AuthenticationStrategy strategy = authenticationStrategyFactory.getStrategy(LoginType.WECHAT);
            result = strategy.authenticate(
                    LoginType.WECHAT.getCode(),
                    wechatReq.code(),
                    wechatReq.state(),
                    wechatReq.userDomain().getCode(),
                    ipAddress,
                    userAgent
            );
        } else {
            // 传统登录（用户名/手机/邮箱 + 密码/验证码）
            LoginStrategy<?> strategy = loginStrategyFactory.getStrategy(request.loginType());
            // 此处的类型转换是安全的，因为工厂根据loginType返回对应的策略
            result = ((LoginStrategy<LoginRequest>) strategy).execute(request, ipAddress, userAgent);
        }

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
                result.refreshTokenExpiresAt()
        );

        log.info("[应用服务] 登录成功: userId={}, loginType={}", result.userId(), request.loginType());
        return response;
    }
}
