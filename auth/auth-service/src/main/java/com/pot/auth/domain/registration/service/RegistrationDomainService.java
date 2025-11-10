package com.pot.auth.domain.registration.service;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.port.CachePort;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.registration.entity.RegistrationRequest;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.LoginContext;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 注册领域服务
 *
 * <p>负责用户注册的核心业务逻辑
 *
 * @author yecao
 * @since 2025-11-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationDomainService {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;
    private final JwtTokenService jwtTokenService;
    private final CachePort cachePort;

    /**
     * 用户名注册
     *
     * <p>最简单的注册方式，不需要验证码
     *
     * @param request 注册请求
     * @return 认证结果（注册后自动登录）
     */
    public AuthenticationResult registerWithUsername(RegistrationRequest request) {
        log.info("[领域服务] 用户名注册: username={}, userDomain={}",
                request.username(), request.userDomain());

        // 1. 获取用户模块适配器
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());

        // 2. 检查用户名是否已存在
        if (userModulePort.existsByUsername(request.username())) {
            throw new DomainException(AuthResultCode.USERNAME_ALREADY_EXISTS);
        }

        // 3. 创建用户命令
        CreateUserCommand createCommand = CreateUserCommand.builder()
                .username(request.username())
                .password(request.password())
                .build();

        // 4. 调用用户模块创建用户
        UserId userId = userModulePort.createUser(createCommand);
        log.info("[领域服务] 用户创建成功: userId={}", userId.value());

        // 5. 查询完整用户信息
        Optional<UserDTO> userOpt = userModulePort.findById(userId);
        if (userOpt.isEmpty()) {
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }
        UserDTO user = userOpt.get();

        // 6. 生成Token（注册后自动登录）
        return generateAuthenticationResult(user, request.userDomain(), request.loginContext());
    }

    /**
     * 邮箱注册
     *
     * @param request 注册请求
     * @return 认证结果（注册后自动登录）
     */
    public AuthenticationResult registerWithEmail(RegistrationRequest request) {
        log.info("[领域服务] 邮箱注册: email={}, userDomain={}",
                request.email().value(), request.userDomain());

        // 1. 验证验证码
        boolean codeValid = verificationCodeService.verifyCode(
                request.email().value(),
                request.verificationCode()
        );
        if (!codeValid) {
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }

        // 2. 获取用户模块适配器
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());

        // 3. 检查邮箱是否已存在
        if (userModulePort.existsByEmail(request.email())) {
            throw new DomainException(AuthResultCode.EMAIL_ALREADY_EXISTS);
        }

        // 4. 生成唯一用户名（基于邮箱前缀 + 随机数）
        String username = generateUsernameFromEmail(request.email().value(), userModulePort);

        // 5. 创建用户命令
        CreateUserCommand createCommand = CreateUserCommand.builder()
                .username(username)
                .email(request.email())
                .password(request.password())
                .emailVerified(true) // 验证码通过，标记为已验证
                .build();

        // 6. 调用用户模块创建用户
        UserId userId = userModulePort.createUser(createCommand);
        log.info("[领域服务] 用户创建成功: userId={}, username={}", userId.value(), username);

        // 7. 查询完整用户信息
        Optional<UserDTO> userOpt = userModulePort.findById(userId);
        if (userOpt.isEmpty()) {
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }
        UserDTO user = userOpt.get();

        // 8. 删除已使用的验证码
        verificationCodeService.deleteCode(request.email().value());

        // 9. 生成Token（注册后自动登录）
        return generateAuthenticationResult(user, request.userDomain(), request.loginContext());
    }

    /**
     * 手机号注册
     *
     * @param request 注册请求
     * @return 认证结果（注册后自动登录）
     */
    public AuthenticationResult registerWithPhone(RegistrationRequest request) {
        log.info("[领域服务] 手机号注册: phone={}, userDomain={}",
                request.phone().value(), request.userDomain());

        // 1. 验证验证码
        boolean codeValid = verificationCodeService.verifyCode(
                request.phone().value(),
                request.verificationCode()
        );
        if (!codeValid) {
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }

        // 2. 获取用户模块适配器
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());

        // 3. 检查手机号是否已存在
        if (userModulePort.existsByPhone(request.phone())) {
            throw new DomainException(AuthResultCode.PHONE_ALREADY_EXISTS);
        }

        // 4. 生成唯一用户名（基于手机号后缀 + 随机数）
        String username = generateUsernameFromPhone(request.phone().value(), userModulePort);

        // 5. 创建用户命令
        CreateUserCommand createCommand = CreateUserCommand.builder()
                .username(username)
                .phone(request.phone())
                .password(request.password())
                .build();

        // 6. 调用用户模块创建用户
        UserId userId = userModulePort.createUser(createCommand);
        log.info("[领域服务] 用户创建成功: userId={}, username={}", userId.value(), username);

        // 7. 查询完整用户信息
        Optional<UserDTO> userOpt = userModulePort.findById(userId);
        if (userOpt.isEmpty()) {
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }
        UserDTO user = userOpt.get();

        // 8. 删除已使用的验证码
        verificationCodeService.deleteCode(request.phone().value());

        // 9. 生成Token（注册后自动登录）
        return generateAuthenticationResult(user, request.userDomain(), request.loginContext());
    }

    /**
     * 生成认证结果
     */
    private AuthenticationResult generateAuthenticationResult(
            UserDTO user,
            UserDomain userDomain,
            LoginContext loginContext
    ) {
        // 生成Token
        var tokenPair = jwtTokenService.generateTokenPair(
                user.userId(),
                userDomain,
                user.username(),
                user.permissions()
        );

        // 构建认证结果
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
     * 从邮箱生成唯一用户名
     * <p>策略：邮箱前缀 + 随机6位数字
     */
    private String generateUsernameFromEmail(String email, UserModulePort userModulePort) {
        String prefix = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");

        // 如果前缀太短，使用默认前缀
        if (prefix.length() < 3) {
            prefix = "user";
        }

        // 限制前缀长度
        if (prefix.length() > 20) {
            prefix = prefix.substring(0, 20);
        }

        // 生成唯一用户名
        String username = prefix;
        int suffix = (int) (Math.random() * 1000000);

        while (userModulePort.existsByUsername(username + suffix)) {
            suffix = (int) (Math.random() * 1000000);
        }

        return username + suffix;
    }

    /**
     * 从手机号生成唯一用户名
     * <p>策略："user" + 手机号后6位 + 随机3位数字
     */
    private String generateUsernameFromPhone(String phone, UserModulePort userModulePort) {
        String phoneSuffix = phone.substring(Math.max(0, phone.length() - 6));
        String username = "user" + phoneSuffix;
        int suffix = (int) (Math.random() * 1000);

        while (userModulePort.existsByUsername(username + suffix)) {
            suffix = (int) (Math.random() * 1000);
        }

        return username + suffix;
    }
}

