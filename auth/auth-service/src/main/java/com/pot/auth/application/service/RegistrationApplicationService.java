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
 * 注册应用服务（重构版）
 *
 * <p>
 * 编排注册流程，根据注册类型自动选择对应的策略
 * <p>
 * 支持6种注册方式：
 * <ul>
 * <li>昵称密码注册</li>
 * <li>邮箱密码注册</li>
 * <li>手机号验证码注册</li>
 * <li>邮箱验证码注册</li>
 * <li>OAuth2注册（Google, GitHub等）- 通过 OneStopAuth 处理</li>
 * <li>微信注册 - 通过 OneStopAuth 处理</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationApplicationService {

        private final RegisterStrategyFactory registerStrategyFactory;
        private final ValidationChain<RegistrationContext> registrationValidationChain;
        private final OneStopAuthenticationService oneStopAuthenticationService;

        /**
         * 统一注册入口
         *
         * <p>
         * 根据注册类型自动选择对应的策略执行注册
         *
         * @param request   注册请求（多态）
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
                        // OAuth2注册 → 使用 OneStopAuth 处理
                        OAuth2AuthRequest authRequest = new OAuth2AuthRequest(
                                        AuthType.OAUTH2,
                                        OAuth2AuthRequest.OAuth2Provider
                                                        .valueOf(request.oauth2ProviderCode().toUpperCase()),
                                        request.code(),
                                        request.state(),
                                        request.userDomain());

                        OneStopAuthResponse authResponse = oneStopAuthenticationService.authenticate(
                                        authRequest, ipAddress, userAgent);

                        // 转换为 RegisterResponse
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
                        // 微信注册 → 使用 OneStopAuth 处理
                        WeChatAuthRequest authRequest = new WeChatAuthRequest(
                                        AuthType.WECHAT,
                                        request.code(),
                                        request.state(),
                                        request.userDomain());

                        OneStopAuthResponse authResponse = oneStopAuthenticationService.authenticate(
                                        authRequest, ipAddress, userAgent);

                        // 转换为 RegisterResponse
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
                        // 传统注册（昵称/手机/邮箱 + 密码/验证码）
                        // 构建注册上下文
                        RegistrationContext context = RegistrationContext.builder()
                                        .request(toCommand(request))
                                        .ipAddress(IpAddress.of(ipAddress))
                                        .deviceInfo(DeviceInfo.fromUserAgent(userAgent != null ? userAgent : "Unknown"))
                                        .build();

                        registrationValidationChain.validate(context);

                        RegisterStrategy strategy = registerStrategyFactory.getStrategy(request.registerType());
                        result = strategy.execute(context);

                        // 转换为应用层DTO
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
