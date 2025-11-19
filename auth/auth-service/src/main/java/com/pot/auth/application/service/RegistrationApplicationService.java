package com.pot.auth.application.service;

import com.pot.auth.application.dto.RegisterResponse;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.strategy.AuthenticationStrategy;
import com.pot.auth.domain.strategy.RegisterStrategy;
import com.pot.auth.domain.strategy.factory.AuthenticationStrategyFactory;
import com.pot.auth.domain.strategy.factory.RegisterStrategyFactory;
import com.pot.auth.interfaces.dto.auth.OAuth2RegisterRequest;
import com.pot.auth.interfaces.dto.auth.RegisterRequest;
import com.pot.auth.interfaces.dto.auth.WeChatRegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 注册应用服务
 *
 * <p>编排注册流程，根据注册类型自动选择对应的策略
 * <p>支持7种注册方式：
 * <ul>
 *   <li>用户名密码注册</li>
 *   <li>手机号密码注册</li>
 *   <li>邮箱密码注册</li>
 *   <li>手机号验证码注册</li>
 *   <li>邮箱验证码注册</li>
 *   <li>OAuth2注册（Google, GitHub等）</li>
 *   <li>微信注册</li>
 * </ul>
 *
 * @author yecao
 * @since 2025-11-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationApplicationService {

    private final RegisterStrategyFactory registerStrategyFactory;
    private final AuthenticationStrategyFactory authenticationStrategyFactory;

    /**
     * 统一注册入口
     *
     * <p>根据注册类型自动选择对应的策略执行注册
     *
     * @param request 注册请求（多态）
     * @param ipAddress 客户端IP地址
     * @param userAgent 用户代理信息
     * @return 注册响应
     */
    public RegisterResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        log.info("[应用服务] 注册请求: registerType={}, userDomain={}",
                request.registerType(), request.userDomain());

        AuthenticationResult result;

        // 判断是传统注册还是一体化认证（OAuth2/WeChat）
        if (RegisterType.OAUTH2.equals(request.registerType())) {
            // OAuth2注册（实际是认证）
            OAuth2RegisterRequest oauthReq = (OAuth2RegisterRequest) request;
            AuthenticationStrategy strategy = authenticationStrategyFactory.getStrategy(RegisterType.OAUTH2);
            result = strategy.authenticate(
                    oauthReq.provider().getCode(),
                    oauthReq.code(),
                    oauthReq.state(),
                    oauthReq.userDomain().getCode(),
                    ipAddress,
                    userAgent
            );
        } else if (RegisterType.WECHAT.equals(request.registerType())) {
            // 微信注册（实际是认证）
            WeChatRegisterRequest wechatReq = (WeChatRegisterRequest) request;
            AuthenticationStrategy strategy = authenticationStrategyFactory.getStrategy(RegisterType.WECHAT);
            result = strategy.authenticate(
                    RegisterType.WECHAT.getCode(),
                    wechatReq.code(),
                    wechatReq.state(),
                    wechatReq.userDomain().getCode(),
                    ipAddress,
                    userAgent
            );
        } else {
            // 传统注册（用户名/手机/邮箱 + 密码/验证码）
            RegisterStrategy strategy = registerStrategyFactory.getStrategy(request.registerType());
            result = strategy.execute(request, ipAddress, userAgent);
        }

        // 转换为应用层DTO
        RegisterResponse response = RegisterResponse.success(
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

        log.info("[应用服务] 注册成功: userId={}, registerType={}", result.userId(), request.registerType());
        return response;
    }
}
