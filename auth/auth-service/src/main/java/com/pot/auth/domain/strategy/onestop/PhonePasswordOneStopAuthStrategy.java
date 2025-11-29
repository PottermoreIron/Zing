package com.pot.auth.domain.strategy.onestop;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.context.OneStopAuthContext;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.domain.shared.valueobject.Phone;
import com.pot.auth.domain.shared.valueobject.VerificationCode;
import com.pot.auth.domain.strategy.AbstractOneStopAuthStrategyImpl;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.interfaces.dto.onestop.PhonePasswordAuthRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 手机号密码一键认证策略
 *
 * <p>
 * 业务流程：
 * <ol>
 * <li>查找用户</li>
 * <li>用户已存在 → 验证密码 → 登录</li>
 * <li>用户不存在 → 验证验证码 → 创建用户（使用提供的密码或自动生成） → 登录</li>
 * </ol>
 *
 * <p>
 * 请求示例（用户已存在，登录）：
 *
 * <pre>
 * {
 *   "authType": "PHONE_PASSWORD",
 *   "phone": "13800138000",
 *   "password": "Password123!",
 *   "userDomain": "MEMBER"
 * }
 * </pre>
 *
 * <p>
 * 请求示例（用户不存在，注册）：
 *
 * <pre>
 * {
 *   "authType": "PHONE_PASSWORD",
 *   "phone": "13900139000",
 *   "password": "Password123!",  // 可选，不提供则自动生成
 *   "verificationCode": "123456",  // 注册时必需
 *   "userDomain": "MEMBER"
 * }
 * </pre>
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Component
public class PhonePasswordOneStopAuthStrategy
        extends AbstractOneStopAuthStrategyImpl<PhonePasswordAuthRequest> {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;

    public PhonePasswordOneStopAuthStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory,
            VerificationCodeService verificationCodeService,
            UserDefaultsGenerator userDefaultsGenerator) {
        super(jwtTokenService, createValidationChain(), userDefaultsGenerator);
        this.userModulePortFactory = userModulePortFactory;
        this.verificationCodeService = verificationCodeService;
    }

    private static ValidationChain<OneStopAuthContext> createValidationChain() {
        ValidationChain<OneStopAuthContext> chain = new ValidationChain<>();
        // 可以添加校验器
        return chain;
    }

    @Override
    protected UserDTO findUser(OneStopAuthContext context) {
        PhonePasswordAuthRequest request = (PhonePasswordAuthRequest) context.request();

        log.debug("[手机号密码认证] 查找用户: phone={}", request.phone());

        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        return userModulePort.findByPhone(request.phone()).orElse(null);
    }

    @Override
    protected void validateCredentialForLogin(OneStopAuthContext context, UserDTO user) {
        PhonePasswordAuthRequest request = (PhonePasswordAuthRequest) context.request();

        log.debug("[手机号密码认证] 验证登录密码: phone={}", request.phone());

        // 登录时必须提供密码
        if (!StringUtils.hasText(request.password())) {
            log.warn("[手机号密码认证] 登录时未提供密码: phone={}", request.phone());
            throw new DomainException(AuthResultCode.AUTHENTICATION_FAILED);
        }

        // 验证密码
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        Optional<UserDTO> authResult = userModulePort.authenticateWithPassword(
                request.phone(), request.password());

        if (authResult.isEmpty()) {
            log.warn("[手机号密码认证] 密码验证失败: phone={}", request.phone());
            throw new DomainException(AuthResultCode.AUTHENTICATION_FAILED);
        }

        log.debug("[手机号密码认证] 登录密码验证通过: phone={}", request.phone());
    }

    @Override
    protected void validateCredentialForRegister(OneStopAuthContext context) {
        PhonePasswordAuthRequest request = (PhonePasswordAuthRequest) context.request();

        log.debug("[手机号密码认证] 验证注册凭证: phone={}", request.phone());

        // 注册时必须验证验证码
        if (!StringUtils.hasText(request.verificationCode())) {
            log.warn("[手机号密码认证] 注册时未提供验证码: phone={}", request.phone());
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }

        boolean codeValid = verificationCodeService.verifyCode(
                request.phone(),
                VerificationCode.of(request.verificationCode()));

        if (!codeValid) {
            log.warn("[手机号密码认证] 验证码无效: phone={}", request.phone());
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }

        log.debug("[手机号密码认证] 注册验证码验证通过: phone={}", request.phone());
    }

    @Override
    protected UserDTO createUserWithDefaults(OneStopAuthContext context) {
        PhonePasswordAuthRequest request = (PhonePasswordAuthRequest) context.request();

        log.info("[手机号密码认证] 创建用户: phone={}", request.phone());

        // 生成默认值
        String username = userDefaultsGenerator.generateUsernameFromPhone(request.phone());
        String password = StringUtils.hasText(request.password())
                ? request.password()
                : userDefaultsGenerator.generateRandomPassword();
        String avatarUrl = userDefaultsGenerator.getDefaultAvatarUrl();

        log.info("[手机号密码认证] 生成默认值: username={}, hasProvidedPassword={}, avatarUrl={}",
                username, StringUtils.hasText(request.password()), avatarUrl);

        // 创建用户
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        CreateUserCommand command = CreateUserCommand.builder()
                .phone(Phone.of(request.phone()))
                .password(Password.of(password))
                .username(username)
                .avatarUrl(avatarUrl)
                .build();

        var userId = userModulePort.createUser(command);
        log.info("[手机号密码认证] 用户创建成功: userId={}", userId.value());

        // 查询完整用户信息
        Optional<UserDTO> userOpt = userModulePort.findById(userId);
        if (userOpt.isEmpty()) {
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }

        return userOpt.get();
    }

    @Override
    protected void afterRegister(UserDTO user, OneStopAuthContext context) {
        PhonePasswordAuthRequest request = (PhonePasswordAuthRequest) context.request();

        // 注册成功后清理验证码
        if (StringUtils.hasText(request.verificationCode())) {
            verificationCodeService.deleteCode(request.phone());
            log.debug("[手机号密码认证] 已清理验证码: phone={}", request.phone());
        }
    }

    @Override
    public AuthType getSupportedAuthType() {
        return AuthType.PHONE_PASSWORD;
    }
}
