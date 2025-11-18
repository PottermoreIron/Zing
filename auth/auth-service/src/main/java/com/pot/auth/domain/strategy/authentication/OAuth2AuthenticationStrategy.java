package com.pot.auth.domain.strategy.authentication;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.oauth2.entity.OAuth2UserInfo;
import com.pot.auth.domain.oauth2.valueobject.OAuth2AuthorizationCode;
import com.pot.auth.domain.oauth2.valueobject.OAuth2Provider;
import com.pot.auth.domain.port.OAuth2Port;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.*;
import com.pot.auth.domain.strategy.AuthenticationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * OAuth2认证策略
 *
 * <p>支持Google、GitHub、Facebook、Apple等OAuth2提供商
 * <p>自动处理注册或登录逻辑：
 * <ul>
 *   <li>若用户已绑定OAuth2账号，则直接登录</li>
 *   <li>若用户未绑定，则自动创建新用户并登录</li>
 * </ul>
 *
 * @author yecao
 * @since 2025-11-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationStrategy implements AuthenticationStrategy {

    private final OAuth2Port oauth2Port;
    private final UserModulePortFactory userModulePortFactory;
    private final JwtTokenService jwtTokenService;

    @Override
    public AuthenticationResult authenticate(
            String provider,
            String authorizationCode,
            String state,
            String userDomain,
            String ipAddress,
            String userAgent
    ) {
        log.info("[OAuth2认证] 开始认证: provider={}, userDomain={}", provider, userDomain);

        try {
            // 1. 构建值对象
            OAuth2Provider oauth2Provider = OAuth2Provider.fromCode(provider);
            OAuth2AuthorizationCode code = OAuth2AuthorizationCode.of(authorizationCode);
            UserDomain domain = UserDomain.fromCode(userDomain);

            // 2. 通过授权码获取OAuth2用户信息
            OAuth2UserInfo oauth2UserInfo = oauth2Port.getUserInfo(oauth2Provider, code, null);
            log.info("[OAuth2认证] 获取OAuth2用户信息成功: openId={}, email={}",
                    oauth2UserInfo.getOpenId().value(), oauth2UserInfo.getEmail());

            // 3. 获取用户模块适配器
            UserModulePort userModulePort = userModulePortFactory.getPort(domain);

            // 4. 查找是否已有绑定的用户
            Optional<UserId> existingUserIdOpt = userModulePort.findUserIdByOAuth2(
                    oauth2Provider.getCode(),
                    oauth2UserInfo.getOpenId().value()
            );

            UserDTO user;
            if (existingUserIdOpt.isPresent()) {
                // 已绑定用户，直接登录
                log.info("[OAuth2认证] 找到已绑定用户: userId={}", existingUserIdOpt.get());
                user = getUserInfo(userModulePort, existingUserIdOpt.get());
            } else {
                // 未绑定用户，创建新用户
                log.info("[OAuth2认证] 未找到绑定用户，创建新用户");
                user = createUserFromOAuth2(oauth2UserInfo, oauth2Provider, userModulePort);
            }

            // 5. 检查账户状态
            validateUserStatus(user);

            // 6. 生成认证结果
            return generateAuthenticationResult(user, domain, ipAddress, userAgent);

        } catch (Exception e) {
            log.error("[OAuth2认证] 认证失败: provider={}, error={}", provider, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean supports(String authenticationType) {
        return "OAUTH2".equals(authenticationType);
    }

    /**
     * 从OAuth2信息创建新用户
     */
    private UserDTO createUserFromOAuth2(
            OAuth2UserInfo oauth2UserInfo,
            OAuth2Provider provider,
            UserModulePort userModulePort
    ) {
        CreateUserCommand createCommand = CreateUserCommand.builder()
                .username(oauth2UserInfo.getUsername())
                .email(oauth2UserInfo.getEmail() != null ? Email.of(oauth2UserInfo.getEmail()) : null)
                .avatarUrl(oauth2UserInfo.getAvatarUrl())
                .oauth2Provider(provider.getCode())
                .oauth2OpenId(oauth2UserInfo.getOpenId().value())
                .build();

        UserId userId = userModulePort.createUser(createCommand);
        log.info("[OAuth2认证] 用户创建成功: userId={}", userId.value());

        return getUserInfo(userModulePort, userId);
    }

    /**
     * 获取用户信息
     */
    private UserDTO getUserInfo(UserModulePort userModulePort, UserId userId) {
        Optional<UserDTO> userOpt = userModulePort.findById(userId);
        if (userOpt.isEmpty()) {
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }
        return userOpt.get();
    }

    /**
     * 验证用户状态
     */
    private void validateUserStatus(UserDTO user) {
        if ("LOCKED".equals(user.status()) || "DISABLED".equals(user.status())) {
            log.warn("[OAuth2认证] 用户账户状态异常: userId={}, status={}", user.userId(), user.status());
            throw new DomainException(AuthResultCode.ACCOUNT_DISABLED);
        }
    }

    /**
     * 生成认证结果
     */
    private AuthenticationResult generateAuthenticationResult(
            UserDTO user,
            UserDomain userDomain,
            String ipAddress,
            String userAgent
    ) {
        // 构建登录上下文
        IpAddress ip = IpAddress.of(ipAddress);
        DeviceInfo deviceInfo = DeviceInfo.fromUserAgent(userAgent != null ? userAgent : "Unknown");
        LoginContext loginContext = LoginContext.of(ip, deviceInfo);

        // 生成Token
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
}

