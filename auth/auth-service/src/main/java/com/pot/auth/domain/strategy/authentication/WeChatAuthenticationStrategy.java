package com.pot.auth.domain.strategy.authentication;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.authentication.service.JwtTokenService;
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
 * 微信认证策略
 *
 * <p>处理微信登录，自动处理注册或登录逻辑：
 * <ul>
 *   <li>若用户已绑定微信账号，则直接登录</li>
 *   <li>若用户未绑定，则自动创建新用户并登录</li>
 * </ul>
 *
 * @author yecao
 * @since 2025-11-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeChatAuthenticationStrategy implements AuthenticationStrategy {

    private final UserModulePortFactory userModulePortFactory;
    private final JwtTokenService jwtTokenService;
    // TODO: 需要添加 WeChatPort 用于获取微信用户信息

    @Override
    public AuthenticationResult authenticate(
            String provider,
            String authorizationCode,
            String state,
            String userDomain,
            String ipAddress,
            String userAgent
    ) {
        log.info("[微信认证] 开始认证: userDomain={}", userDomain);

        try {
            // 1. 构建值对象
            UserDomain domain = UserDomain.fromCode(userDomain);

            // 2. 通过授权码获取微信用户信息
            // TODO: 调用微信API获取用户信息
            String wechatOpenId = getWeChatOpenId(authorizationCode);
            WeChatUserInfo wechatUserInfo = getWeChatUserInfo(authorizationCode);

            // 3. 获取用户模块适配器
            UserModulePort userModulePort = userModulePortFactory.getPort(domain);

            // 4. 查找是否已有绑定的用户
//            Optional<UserId> existingUserIdOpt = userModulePort.findUserIdByWeChat(wechatOpenId);
            Optional<UserId> existingUserIdOpt = Optional.empty(); // TODO: 实现查找逻辑

            UserDTO user;
            if (existingUserIdOpt.isPresent()) {
                // 已绑定用户，直接登录
                log.info("[微信认证] 找到已绑定用户: userId={}", existingUserIdOpt.get());
                user = getUserInfo(userModulePort, existingUserIdOpt.get());
            } else {
                // 未绑定用户，创建新用户
                log.info("[微信认证] 未找到绑定用户，创建新用户");
                user = createUserFromWeChat(wechatUserInfo, wechatOpenId, userModulePort);
            }

            // 5. 检查账户状态
            validateUserStatus(user);

            // 6. 生成认证结果
            return generateAuthenticationResult(user, domain, ipAddress, userAgent);

        } catch (Exception e) {
            log.error("[微信认证] 认证失败: error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public boolean supports(String authenticationType) {
        return "WECHAT".equals(authenticationType);
    }

    /**
     * 获取微信OpenId
     * TODO: 实现微信API调用
     */
    private String getWeChatOpenId(String code) {
        // 临时实现，需要调用微信API
        return "wx_temp_openid";
    }

    /**
     * 获取微信用户信息
     * TODO: 实现微信API调用
     */
    private WeChatUserInfo getWeChatUserInfo(String code) {
        // 临时实现，需要调用微信API
        return new WeChatUserInfo("微信用户", null, null);
    }

    /**
     * 从微信信息创建新用户
     */
    private UserDTO createUserFromWeChat(
            WeChatUserInfo wechatUserInfo,
            String wechatOpenId,
            UserModulePort userModulePort
    ) {
        CreateUserCommand createCommand = CreateUserCommand.builder()
                .username(wechatUserInfo.nickname())
                .avatarUrl(wechatUserInfo.avatar())
                .oauth2OpenId(wechatOpenId)
                .build();

        UserId userId = userModulePort.createUser(createCommand);
        log.info("[微信认证] 用户创建成功: userId={}", userId.value());

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
            log.warn("[微信认证] 用户账户状态异常: userId={}, status={}", user.userId(), user.status());
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

    /**
     * 微信用户信息临时记录类
     * TODO: 移到合适的包
     */
    private record WeChatUserInfo(String nickname, String avatar, String unionId) {
    }
}

