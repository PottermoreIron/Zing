package com.pot.auth.domain.oauth2.service;

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
import com.pot.auth.domain.shared.valueobject.Email;
import com.pot.auth.domain.shared.valueobject.LoginContext;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * OAuth2领域服务
 *
 * <p>负责OAuth2登录的核心业务逻辑
 *
 * @author yecao
 * @since 2025-11-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2DomainService {

    private final OAuth2Port oauth2Port;
    private final UserModulePortFactory userModulePortFactory;
    private final JwtTokenService jwtTokenService;

    /**
     * OAuth2登录
     *
     * @param provider     OAuth2提供商
     * @param code         授权码
     * @param redirectUri  回调地址
     * @param userDomain   用户域
     * @param loginContext 登录上下文
     * @return 认证结果
     */
    public AuthenticationResult loginWithOAuth2(
            OAuth2Provider provider,
            OAuth2AuthorizationCode code,
            String redirectUri,
            UserDomain userDomain,
            LoginContext loginContext
    ) {
        log.info("[OAuth2] 开始OAuth2登录: provider={}, userDomain={}", provider, userDomain);

        // 1. 通过授权码获取OAuth2用户信息
        OAuth2UserInfo oauth2UserInfo = oauth2Port.getUserInfo(provider, code, redirectUri);
        log.info("[OAuth2] 获取OAuth2用户信息成功: openId={}, email={}",
                oauth2UserInfo.getOpenId().value(), oauth2UserInfo.getEmail());

        // 2. 获取用户模块适配器
        UserModulePort userModulePort = userModulePortFactory.getPort(userDomain);

        // 3. 查找是否已有绑定的用户
        Optional<UserId> existingUserIdOpt = userModulePort.findUserIdByOAuth2(
                provider.getCode(),
                oauth2UserInfo.getOpenId().value()
        );

        UserDTO user;
        if (existingUserIdOpt.isPresent()) {
            // 3.1 已绑定用户，直接登录
            log.info("[OAuth2] 找到已绑定用户: userId={}", existingUserIdOpt.get());
            Optional<UserDTO> userOpt = userModulePort.findById(existingUserIdOpt.get());
            if (userOpt.isEmpty()) {
                throw new DomainException(AuthResultCode.USER_NOT_FOUND);
            }
            user = userOpt.get();
        } else {
            // 3.2 未绑定用户，创建新用户
            log.info("[OAuth2] 未找到绑定用户，创建新用户");
            user = createUserFromOAuth2(oauth2UserInfo, provider, userDomain, userModulePort);
        }

        // 4. 检查账户状态
        if ("suspended".equals(user.status()) || "inactive".equals(user.status())) {
            throw new DomainException(AuthResultCode.ACCOUNT_DISABLED);
        }

        // 5. 生成Token
        var tokenPair = jwtTokenService.generateTokenPair(
                user.userId(),
                userDomain,
                user.username(),
                user.permissions()
        );

        // 6. 构建认证结果
        AuthenticationResult result = AuthenticationResult.builder()
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

        log.info("[OAuth2] OAuth2登录成功: userId={}", user.userId());
        return result;
    }

    /**
     * 从OAuth2信息创建新用户
     */
    private UserDTO createUserFromOAuth2(
            OAuth2UserInfo oauth2UserInfo,
            OAuth2Provider provider,
            UserDomain userDomain,
            UserModulePort userModulePort
    ) {
        // 1. 生成用户名（如果OAuth2没有提供）
        String username = oauth2UserInfo.getUsername();
        if (username == null || username.isBlank()) {
            username = generateUsernameFromOAuth2(provider, oauth2UserInfo);
        }

        // 2. 检查用户名是否已存在，存在则添加后缀
        String finalUsername = username;
        int suffix = 1;
        while (userModulePort.existsByUsername(finalUsername)) {
            finalUsername = username + "_" + suffix++;
        }

        // 3. 创建用户命令
        CreateUserCommand createCommand = CreateUserCommand.builder()
                .username(finalUsername)
                .email(Email.of(oauth2UserInfo.getEmail()))
                .password(null) // OAuth2用户无需密码
                .emailVerified(oauth2UserInfo.getEmailVerified() != null && oauth2UserInfo.getEmailVerified())
                .firstName(oauth2UserInfo.getNickname())
                .lastName("")
                .build();

        // 4. 创建用户
        UserId userId = userModulePort.createUser(createCommand);
        log.info("[OAuth2] 新用户创建成功: userId={}, provider={}", userId.value(), provider);

        // 5. 绑定OAuth2账号
        Map<String, Object> oauth2Data = Map.of(
                "accessToken", oauth2UserInfo.getAccessToken(),
                "refreshToken", oauth2UserInfo.getRefreshToken() != null ? oauth2UserInfo.getRefreshToken() : "",
                "expiresIn", oauth2UserInfo.getExpiresIn() != null ? oauth2UserInfo.getExpiresIn() : 0L,
                "nickname", oauth2UserInfo.getNickname() != null ? oauth2UserInfo.getNickname() : "",
                "avatarUrl", oauth2UserInfo.getAvatarUrl() != null ? oauth2UserInfo.getAvatarUrl() : ""
        );

        userModulePort.bindOAuth2(
                userId,
                provider.getCode(),
                oauth2UserInfo.getOpenId().value(),
                oauth2Data
        );

        // 6. 查询完整用户信息
        Optional<UserDTO> userOpt = userModulePort.findById(userId);
        if (userOpt.isEmpty()) {
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }

        return userOpt.get();
    }

    /**
     * 从OAuth2信息生成用户名
     */
    private String generateUsernameFromOAuth2(OAuth2Provider provider, OAuth2UserInfo oauth2UserInfo) {
        String base = oauth2UserInfo.getNickname();
        if (base == null || base.isBlank()) {
            base = provider.getCode() + "_user";
        }
        // 只保留字母、数字、下划线
        base = base.replaceAll("[^a-zA-Z0-9_]", "_");
        return base.toLowerCase();
    }

    /**
     * 获取OAuth2授权URL
     */
    public String getAuthorizationUrl(OAuth2Provider provider, String state, String redirectUri) {
        return oauth2Port.getAuthorizationUrl(provider, state, redirectUri);
    }
}

