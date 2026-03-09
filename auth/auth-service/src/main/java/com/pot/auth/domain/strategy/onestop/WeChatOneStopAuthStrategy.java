package com.pot.auth.domain.strategy.onestop;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.context.OneStopAuthContext;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.WeChatPort;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.domain.strategy.AbstractOneStopAuthStrategyImpl;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.domain.wechat.entity.WeChatUserInfo;
import com.pot.auth.interfaces.dto.onestop.WeChatAuthRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 微信一键认证策略
 *
 * <p>
 * 支持微信扫码登录
 *
 * <p>
 * 认证流程：
 * <ol>
 * <li>通过授权码获取微信用户信息</li>
 * <li>根据 weChatOpenId 查找用户</li>
 * <li>用户已存在 → 直接登录</li>
 * <li>用户不存在 → 创建用户并绑定微信 → 登录</li>
 * </ol>
 *
 * @author pot
 * @since 2025-11-30
 */
@Slf4j
@Component
public class WeChatOneStopAuthStrategy
        extends AbstractOneStopAuthStrategyImpl<WeChatAuthRequest> {

    private final WeChatPort weChatPort;
    private final UserModulePortFactory userModulePortFactory;

    /**
     * 线程级微信用户信息缓存
     *
     * <p>
     * 使用 ThreadLocal 替代实例字段，确保并发安全：每个请求线程拥有独立副本，
     * 避免多并发请求互相覆盖。在 {@code cleanupAfterAuthentication()} 中显式清理，
     * 防止线程池复用时的内存泄漏。
     */
    private static final ThreadLocal<WeChatUserInfo> USER_INFO_CACHE = new ThreadLocal<>();

    public WeChatOneStopAuthStrategy(
            JwtTokenService jwtTokenService,
            WeChatPort weChatPort,
            UserModulePortFactory userModulePortFactory,
            UserDefaultsGenerator userDefaultsGenerator) {
        super(jwtTokenService, createValidationChain(), userDefaultsGenerator);
        this.weChatPort = weChatPort;
        this.userModulePortFactory = userModulePortFactory;
    }

    private static ValidationChain<OneStopAuthContext> createValidationChain() {
        return new ValidationChain<>();
    }

    @Override
    protected UserDTO findUser(OneStopAuthContext context) {
        WeChatAuthRequest request = (WeChatAuthRequest) context.request();

        log.info("[微信认证] 查找用户: userDomain={}", request.userDomain());

        try {
            // 1. 获取微信用户信息（并缓存）
            WeChatUserInfo weChatUserInfo = getOrFetchWeChatUserInfo(request);

            // 2. 根据 weChatOpenId 查找用户
            UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
            return userModulePort.findUserByWeChat(weChatUserInfo.getOpenId())
                    .orElse(null);

        } catch (Exception e) {
            log.error("[微信认证] 查找用户失败: error={}", e.getMessage());
            throw new DomainException("获取微信用户信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    protected void validateCredentialForLogin(OneStopAuthContext context, UserDTO user) {
        // 微信认证已通过，无需额外验证
        log.debug("[微信认证] 用户已绑定，直接登录: userId={}", user.userId());

        // 微信登录时，授权码已通过 API 调用验证，无需额外验证
        // 授权码的有效性在 getOrFetchWeChatUserInfo 中已经验证
    }

    @Override
    protected void validateCredentialForRegister(OneStopAuthContext context) {
        log.debug("[微信认证] 验证授权码有效性");

        // 验证授权码有效性（通过调用 WeChatPort 获取用户信息）
        // 如果授权码无效，会抛出异常
        getOrFetchWeChatUserInfo((WeChatAuthRequest) context.request());
    }

    @Override
    protected UserDTO createUserWithDefaults(OneStopAuthContext context) {
        WeChatAuthRequest request = (WeChatAuthRequest) context.request();

        log.info("[微信认证] 创建新用户: userDomain={}", request.userDomain());

        // 1. 获取微信用户信息
        WeChatUserInfo weChatUserInfo = getOrFetchWeChatUserInfo(request);

        // 2. 生成默认值
        // 使用通用的用户名生成方法，基于微信唯一标识
        String username = userDefaultsGenerator.generateUsername();
        String password = userDefaultsGenerator.generateRandomPassword();
        String avatarUrl = weChatUserInfo.getAvatar() != null
                ? weChatUserInfo.getAvatar()
                : userDefaultsGenerator.getDefaultAvatarUrl();

        // 3. 创建用户
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        CreateUserCommand command = CreateUserCommand.builder()
                .username(username)
                .password(Password.of(password))
                .nickname(weChatUserInfo.getDisplayName())
                .avatarUrl(avatarUrl)
                .weChatOpenId(weChatUserInfo.getOpenId())
                .weChatUnionId(weChatUserInfo.getUnionId())
                .build();

        var userId = userModulePort.createUser(command);

        log.info("[微信认证] 用户创建成功: userId={}, openId={}",
                userId, weChatUserInfo.getOpenId());

        // 4. 返回用户信息
        return userModulePort.findById(userId)
                .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
    }

    @Override
    public boolean supports(AuthType authType) {
        return authType == AuthType.WECHAT;
    }

    @Override
    public AuthType getSupportedAuthType() {
        return AuthType.WECHAT;
    }

    /**
     * 清理线程级缓存，防止线程池复用时的内存泄漏
     */
    @Override
    protected void cleanupAfterAuthentication() {
        USER_INFO_CACHE.remove();
    }

    /**
     * 获取或从 ThreadLocal 缓存中读取微信用户信息
     *
     * <p>
     * 避免在同一请求的 findUser、validateCredential、createUser 三步中重复调用微信 API。
     * 线程安全：ThreadLocal 保证每个请求线程有独立副本。
     *
     * @param request 微信认证请求
     * @return 微信用户信息
     */
    private WeChatUserInfo getOrFetchWeChatUserInfo(WeChatAuthRequest request) {
        WeChatUserInfo cached = USER_INFO_CACHE.get();
        if (cached != null) {
            return cached;
        }

        WeChatUserInfo weChatUserInfo = weChatPort.getUserInfo(request.code(), request.state());
        USER_INFO_CACHE.set(weChatUserInfo);

        log.debug("[微信认证] 微信用户信息获取成功: openId={}, nickname={}",
                weChatUserInfo.getOpenId(),
                weChatUserInfo.getNickname());

        return weChatUserInfo;
    }
}
