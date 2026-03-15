package com.pot.auth.domain.strategy.onestop;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.context.OneStopAuthContext;
import com.pot.auth.domain.oauth2.entity.OAuth2UserInfo;
import com.pot.auth.domain.oauth2.valueobject.OAuth2AuthorizationCode;
import com.pot.auth.domain.oauth2.valueobject.OAuth2Provider;
import com.pot.auth.domain.port.OAuth2Port;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import com.pot.auth.domain.shared.valueobject.Email;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.domain.strategy.AbstractOneStopAuthStrategyImpl;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.interfaces.dto.onestop.OAuth2AuthRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * OAuth2 一键认证策略
 *
 * <p>
 * 支持 Google、GitHub、Facebook、Apple、Microsoft 等 OAuth2 提供商
 *
 * <p>
 * 认证流程：
 * <ol>
 * <li>通过授权码获取 OAuth2 用户信息</li>
 * <li>根据 provider + openId 查找用户</li>
 * <li>用户已存在 → 直接登录</li>
 * <li>用户不存在 → 创建用户并绑定 OAuth2 → 登录</li>
 * </ol>
 *
 * @author pot
 * @since 2025-11-30
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "auth.oauth2.enabled", havingValue = "true")
public class OAuth2OneStopAuthStrategy
        extends AbstractOneStopAuthStrategyImpl<OAuth2AuthRequest> {

    private final OAuth2Port oauth2Port;
    private final UserModulePortFactory userModulePortFactory;

    /**
     * 线程级 OAuth2 用户信息缓存
     *
     * <p>
     * 使用 ThreadLocal 替代实例字段，确保并发安全：每个请求线程拥有独立副本，
     * 避免多并发请求互相覆盖。在 {@code cleanupAfterAuthentication()} 中显式清理，
     * 防止线程池复用时的内存泄漏。
     */
    private static final ThreadLocal<OAuth2UserInfo> USER_INFO_CACHE = new ThreadLocal<>();

    public OAuth2OneStopAuthStrategy(
            JwtTokenService jwtTokenService,
            OAuth2Port oauth2Port,
            UserModulePortFactory userModulePortFactory,
            UserDefaultsGenerator userDefaultsGenerator) {
        super(jwtTokenService, createValidationChain(), userDefaultsGenerator);
        this.oauth2Port = oauth2Port;
        this.userModulePortFactory = userModulePortFactory;
    }

    private static ValidationChain<OneStopAuthContext> createValidationChain() {
        return new ValidationChain<>();
    }

    @Override
    protected UserDTO findUser(OneStopAuthContext context) {
        OAuth2AuthRequest request = (OAuth2AuthRequest) context.request();

        log.info("[OAuth2认证] 查找用户: provider={}, userDomain={}",
                request.provider(), request.userDomain());

        try {
            // 1. 获取 OAuth2 用户信息（并缓存）
            OAuth2UserInfo oauth2UserInfo = getOrFetchOAuth2UserInfo(request);

            // 2. 根据 provider + openId 查找用户
            UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
            return userModulePort.findUserByOAuth2(
                    request.provider().getCode(),
                    oauth2UserInfo.openId().value()).orElse(null);

        } catch (Exception e) {
            log.error("[OAuth2认证] 查找用户失败: provider={}, error={}",
                    request.provider(), e.getMessage());
            throw new DomainException("获取OAuth2用户信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    protected void validateCredentialForLogin(OneStopAuthContext context, UserDTO user) {
        OAuth2AuthRequest request = (OAuth2AuthRequest) context.request();

        log.debug("[OAuth2认证] 用户已绑定，直接登录: userId={}, provider={}",
                user.userId(), request.provider());

        // OAuth2 登录时，授权码已通过 API 调用验证，无需额外验证
        // 授权码的有效性在 getOrFetchOAuth2UserInfo 中已经验证
    }

    @Override
    protected void validateCredentialForRegister(OneStopAuthContext context) {
        OAuth2AuthRequest request = (OAuth2AuthRequest) context.request();

        log.debug("[OAuth2认证] 验证授权码有效性: provider={}", request.provider());

        // 验证授权码有效性（通过调用 OAuth2Port 获取用户信息）
        // 如果授权码无效，会抛出异常
        getOrFetchOAuth2UserInfo(request);
    }

    @Override
    protected UserDTO createUserWithDefaults(OneStopAuthContext context) {
        OAuth2AuthRequest request = (OAuth2AuthRequest) context.request();

        log.info("[OAuth2认证] 创建新用户: provider={}, userDomain={}",
                request.provider(), request.userDomain());

        // 1. 获取 OAuth2 用户信息
        OAuth2UserInfo oauth2UserInfo = getOrFetchOAuth2UserInfo(request);

        // 2. 生成默认值
        String username = userDefaultsGenerator.generateUsernameFromEmail(
                oauth2UserInfo.email());
        String password = userDefaultsGenerator.generateRandomPassword();
        String avatarUrl = oauth2UserInfo.avatarUrl() != null
                ? oauth2UserInfo.avatarUrl()
                : userDefaultsGenerator.getDefaultAvatarUrl();

        // 3. 创建用户
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        CreateUserCommand command = CreateUserCommand.builder()
                .username(username)
                .email(Email.of(oauth2UserInfo.email()))
                .password(Password.of(password))
                .nickname(oauth2UserInfo.nickname())
                .avatarUrl(avatarUrl)
                .emailVerified(oauth2UserInfo.emailVerified() != null && oauth2UserInfo.emailVerified())
                .oauth2Provider(request.provider().getCode())
                .oauth2OpenId(oauth2UserInfo.openId().value())
                .build();

        var userId = userModulePort.createUser(command);

        log.info("[OAuth2认证] 用户创建成功: userId={}, provider={}, email={}",
                userId, request.provider(), oauth2UserInfo.email());

        // 4. 返回用户信息
        return userModulePort.findById(userId)
                .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
    }

    @Override
    public boolean supports(AuthType authType) {
        return authType == AuthType.OAUTH2;
    }

    @Override
    public AuthType getSupportedAuthType() {
        return AuthType.OAUTH2;
    }

    /**
     * 清理线程级缓存，防止线程池复用时的内存泄漏
     */
    @Override
    protected void cleanupAfterAuthentication() {
        USER_INFO_CACHE.remove();
    }

    /**
     * 获取或从 ThreadLocal 缓存中读取 OAuth2 用户信息
     *
     * <p>
     * 避免在同一请求的 findUser、validateCredential、createUser 三步中重复调用 OAuth2 API。
     * 线程安全：ThreadLocal 保证每个请求线程有独立副本。
     *
     * @param request OAuth2 认证请求
     * @return OAuth2 用户信息
     */
    private OAuth2UserInfo getOrFetchOAuth2UserInfo(OAuth2AuthRequest request) {
        OAuth2UserInfo cached = USER_INFO_CACHE.get();
        if (cached != null) {
            return cached;
        }

        OAuth2Provider provider = OAuth2Provider.fromCode(request.provider().getCode());
        OAuth2AuthorizationCode code = OAuth2AuthorizationCode.of(request.code());

        OAuth2UserInfo oauth2UserInfo = oauth2Port.getUserInfo(provider, code, request.state());
        USER_INFO_CACHE.set(oauth2UserInfo);

        log.debug("[OAuth2认证] OAuth2用户信息获取成功: openId={}, email={}",
                oauth2UserInfo.openId().value(),
                oauth2UserInfo.email());

        return oauth2UserInfo;
    }
}
