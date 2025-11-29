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
import com.pot.auth.domain.shared.valueobject.Phone;
import com.pot.auth.domain.shared.valueobject.VerificationCode;
import com.pot.auth.domain.strategy.AbstractRegisterStrategyImpl;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.domain.validation.handler.RegistrationParameterValidator;
import com.pot.auth.interfaces.dto.register.PhoneCodeRegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 手机号验证码注册策略（重构版）
 *
 * <p>
 * 使用手机号+验证码注册（无密码）
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Component
public class PhoneCodeRegisterStrategy extends AbstractRegisterStrategyImpl<PhoneCodeRegisterRequest> {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;

    public PhoneCodeRegisterStrategy(
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
        PhoneCodeRegisterRequest request = (PhoneCodeRegisterRequest) context.request();

        log.debug("[手机号验证码注册] 开始验证凭证: phone={}", request.phone());

        // 验证验证码
        boolean codeValid = verificationCodeService.verifyCode(request.phone(), VerificationCode.of(request.verificationCode()));

        if (!codeValid) {
            log.warn("[手机号验证码注册] 验证码无效: phone={}", request.phone());
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }

        log.debug("[手机号验证码注册] 验证码验证通过: phone={}", request.phone());
    }

    @Override
    protected void beforeRegister(RegistrationContext context) {
        PhoneCodeRegisterRequest request = (PhoneCodeRegisterRequest) context.request();

        log.debug("[手机号验证码注册] 注册前置检查: phone={}", request.phone());

        // 检查手机号是否已存在
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        if (userModulePort.existsByPhone(Phone.of(request.phone()))) {
            log.warn("[手机号验证码注册] 手机号已存在: phone={}", request.phone());
            throw new DomainException(AuthResultCode.PHONE_ALREADY_EXISTS);
        }
    }

    @Override
    protected UserDTO createUser(RegistrationContext context) {
        PhoneCodeRegisterRequest request = (PhoneCodeRegisterRequest) context.request();

        log.info("[手机号验证码注册] 创建用户: phone={}", request.phone());

        // 创建用户（无密码注册）
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        Phone phone = Phone.of(request.phone());

        CreateUserCommand createCommand = CreateUserCommand.builder()
                .phone(phone)
                .build();

        var userId = userModulePort.createUser(createCommand);
        log.info("[手机号验证码注册] 用户创建成功: userId={}", userId.value());

        // 查询完整用户信息
        Optional<UserDTO> userOpt = userModulePort.findById(userId);
        if (userOpt.isEmpty()) {
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }

        return userOpt.get();
    }

    @Override
    protected void afterRegister(UserDTO user, RegistrationContext context) {
        PhoneCodeRegisterRequest request = (PhoneCodeRegisterRequest) context.request();

        // 注册成功后清理验证码
        verificationCodeService.deleteCode(request.phone());

        log.debug("[手机号验证码注册] 已清理验证码: phone={}", request.phone());
    }

    @Override
    protected RegisterType getSupportedRegisterType() {
        return RegisterType.PHONE_CODE;
    }
}
