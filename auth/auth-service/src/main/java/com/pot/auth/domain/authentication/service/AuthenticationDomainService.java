package com.pot.auth.domain.authentication.service;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.authentication.valueobject.TokenPair;
import com.pot.auth.domain.port.CachePort;
import com.pot.auth.domain.port.TokenManagementPort;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.dto.DeviceDTO;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.LoginContext;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * 认证领域服务
 *
 * <p>核心认证业务逻辑，包含多种认证方式：
 * <ul>
 *   <li>密码认证</li>
 *   <li>验证码认证（预留）</li>
 *   <li>OAuth2认证（预留）</li>
 *   <li>微信认证（预留）</li>
 * </ul>
 *
 * @author yecao
 * @since 2025-11-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationDomainService {

    private final UserModulePort userModulePort;
    private final TokenManagementPort tokenManagementPort;
    private final JwtTokenService jwtTokenService;
    private final CachePort cachePort;

    /**
     * 密码认证
     *
     * @param identifier   用户标识（用户名/邮箱/手机号）
     * @param password     密码
     * @param userDomain   用户域
     * @param loginContext 登录上下文
     * @return 认证结果
     */
    public AuthenticationResult authenticateWithPassword(
            String identifier,
            Password password,
            UserDomain userDomain,
            LoginContext loginContext
    ) {
        log.info("[认证] 开始密码认证: identifier={}, userDomain={}", identifier, userDomain);

        // 1. 调用用户模块进行密码验证
        Optional<UserDTO> userOpt = userModulePort.authenticateWithPassword(identifier, password.value());

        if (userOpt.isEmpty()) {
            log.warn("[认证] 密码认证失败: identifier={}", identifier);
            throw new AuthenticationFailedException("用户名或密码错误");
        }

        UserDTO user = userOpt.get();

        // 2. 检查用户状态
        if ("LOCKED".equals(user.status())) {
            log.warn("[认证] 用户已被锁定: userId={}", user.userId());
            throw new AccountLockedException("账户已被锁定，请联系管理员");
        }

        if ("DISABLED".equals(user.status())) {
            log.warn("[认证] 用户已被禁用: userId={}", user.userId());
            throw new AccountDisabledException("账户已被禁用");
        }
        // 3. 获取用户权限
        Set<String> authorities = userModulePort.getPermissions(user.userId());// 4. 生成Token对
        TokenPair tokenPair = tokenManagementPort.generateTokenPair(
                user.userId(),
                user.userDomain(),
                user.username(),
                authorities
        );

        // 5. 记录设备登录（存储RefreshToken到Redis）
        // TODO: 需要将DeviceInfo转换为DeviceDTO
        DeviceDTO deviceDTO = DeviceDTO.builder()
                .deviceId(null) // deviceId由member-service生成
                .deviceType(loginContext.deviceInfo().deviceType())
                .platform(loginContext.deviceInfo().osName())
                .browser(loginContext.deviceInfo().browserName())
                .appVersion("Unknown")
                .isActive(true)
                .lastUsedAt(java.time.LocalDateTime.now())
                .build();

        userModulePort.recordDeviceLogin(
                user.userId(),
                deviceDTO,
                loginContext.ipAddress(),
                tokenPair.getRefreshTokenString()
        );

        log.info("[认证] 密码认证成功: userId={}, username={}", user.userId(), user.username());

        // 6. 返回认证结果
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
     * 验证码认证（预留）
     */
    public AuthenticationResult authenticateWithVerificationCode(
            String identifier,
            String code,
            UserDomain userDomain,
            LoginContext loginContext
    ) {
        // TODO: 实现验证码认证
        throw new UnsupportedOperationException("验证码认证功能待实现");
    }

    /**
     * 构建认证结果（供验证码登录等场景使用）
     *
     * @param user         用户信息
     * @param userDomain   用户域
     * @param loginContext 登录上下文
     * @return 认证结果
     */
    public AuthenticationResult buildAuthenticationResult(
            UserDTO user,
            UserDomain userDomain,
            LoginContext loginContext
    ) {
        log.info("[认证] 构建认证结果: userId={}, userDomain={}", user.userId(), userDomain);

        // 1. 生成Token
        TokenPair tokenPair = jwtTokenService.generateTokenPair(
                user.userId(),
                userDomain,
                user.username(),
                user.permissions()
        );

        // 2. 构建认证结果
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
     * 自定义认证异常
     */
    public static class AuthenticationFailedException extends DomainException {
        public AuthenticationFailedException(String message) {
            super(message);
        }
    }

    public static class AccountLockedException extends DomainException {
        public AccountLockedException(String message) {
            super(message);
        }
    }

    public static class AccountDisabledException extends DomainException {
        public AccountDisabledException(String message) {
            super(message);
        }
    }
}

