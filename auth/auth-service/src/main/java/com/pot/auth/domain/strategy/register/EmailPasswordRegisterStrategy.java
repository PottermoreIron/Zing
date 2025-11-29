package com.pot.auth.domain.strategy.register;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.context.RegistrationContext;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.Email;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.domain.shared.valueobject.VerificationCode;
import com.pot.auth.domain.strategy.AbstractRegisterStrategyImpl;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.domain.validation.handler.RegistrationParameterValidator;
import com.pot.auth.interfaces.dto.register.EmailPasswordRegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 邮箱密码注册策略（重构版）
 *
 * <p>
 * 使用邮箱+密码+验证码注册
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Component
public class EmailPasswordRegisterStrategy extends AbstractRegisterStrategyImpl<EmailPasswordRegisterRequest> {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;

    public EmailPasswordRegisterStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory,
            VerificationCodeService verificationCodeService) {
        super(jwtTokenService, createValidationChain());
        this.userModulePortFactory = userModulePortFactory;
        this.verificationCodeService = verificationCodeService;
    }

    private static ValidationChain<RegistrationContext> createValidationChain() {
        ValidationChain<RegistrationContext> chain = new ValidationChain<>();
        chain.addHandler(new RegistrationParameterValidator());
        return chain;
    }

    @Override
    protected void validateCredential(RegistrationContext context) {
        EmailPasswordRegisterRequest request = (EmailPasswordRegisterRequest) context.request();

        log.debug("[邮箱密码注册] 开始验证凭证: email={}", request.email());

        // 验证验证码
        boolean codeValid = verificationCodeService.verifyCode(request.email(), VerificationCode.of(request.verificationCode()));

        if (!codeValid) {
            log.warn("[邮箱密码注册] 验证码无效: email={}", request.email());
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }

        log.debug("[邮箱密码注册] 验证码验证通过: email={}", request.email());
    }

    @Override
    protected void beforeRegister(RegistrationContext context) {
        EmailPasswordRegisterRequest request = (EmailPasswordRegisterRequest) context.request();

        log.debug("[邮箱密码注册] 注册前置检查: email={}", request.email());

        // 检查邮箱是否已存在
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        if (userModulePort.existsByEmail(Email.of(request.email()))) {
            log.warn("[邮箱密码注册] 邮箱已存在: email={}", request.email());
            throw new DomainException(AuthResultCode.EMAIL_ALREADY_EXISTS);
        }
    }

    @Override
    protected UserDTO createUser(RegistrationContext context) {
        EmailPasswordRegisterRequest request = (EmailPasswordRegisterRequest) context.request();

        log.info("[邮箱密码注册] 创建用户: email={}", request.email());

        // 创建用户
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        Email email = Email.of(request.email());
        Password password = Password.of(request.password());

        CreateUserCommand createCommand = CreateUserCommand.builder()
                .email(email)
                .password(password)
                .build();

        var userId = userModulePort.createUser(createCommand);
        log.info("[邮箱密码注册] 用户创建成功: userId={}", userId.value());

        // 查询完整用户信息
        Optional<UserDTO> userOpt = userModulePort.findById(userId);
        if (userOpt.isEmpty()) {
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }

        return userOpt.get();
    }

    @Override
    protected void afterRegister(UserDTO user, RegistrationContext context) {
        EmailPasswordRegisterRequest request = (EmailPasswordRegisterRequest) context.request();

        // 注册成功后清理验证码
        verificationCodeService.deleteCode(request.email());

        log.debug("[邮箱密码注册] 已清理验证码: email={}", request.email());
    }

    @Override
    protected RegisterType getSupportedRegisterType() {
        return RegisterType.EMAIL_PASSWORD;
    }
}
