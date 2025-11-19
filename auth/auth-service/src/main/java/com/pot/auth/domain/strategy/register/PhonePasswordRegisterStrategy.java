package com.pot.auth.domain.strategy.register;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.*;
import com.pot.auth.domain.strategy.AbstractRegisterStrategy;
import com.pot.auth.interfaces.dto.auth.PhonePasswordRegisterRequest;
import com.pot.auth.interfaces.dto.auth.RegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 手机号密码注册策略
 *
 * <p>使用手机号+密码+验证码注册
 *
 * @author yecao
 * @since 2025-11-18
 */
@Slf4j
@Component
public class PhonePasswordRegisterStrategy extends AbstractRegisterStrategy {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;

    public PhonePasswordRegisterStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory,
            VerificationCodeService verificationCodeService
    ) {
        super(jwtTokenService);
        this.userModulePortFactory = userModulePortFactory;
        this.verificationCodeService = verificationCodeService;
    }

    @Override
    protected void validateRequest(RegisterRequest request) {
        if (!(request instanceof PhonePasswordRegisterRequest)) {
            throw new DomainException(AuthResultCode.INVALID_REGISTER_REQUEST);
        }
    }

    @Override
    protected UserDTO doRegister(RegisterRequest request, LoginContext loginContext) {
        PhonePasswordRegisterRequest req = (PhonePasswordRegisterRequest) request;

        log.info("[手机号注册] 开始注册: phone={}", req.phone());

        // 1. 验证验证码
        boolean codeValid = verificationCodeService.verifyCode(
                req.phone(),
                VerificationCode.of(req.verificationCode())
        );
        if (!codeValid) {
            log.warn("[手机号注册] 验证码无效: phone={}", req.phone());
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }

        // 2. 获取用户模块适配器
        UserDomain userDomain = req.userDomain();
        UserModulePort userModulePort = userModulePortFactory.getPort(userDomain);

        // 3. 检查手机号是否已存在
        if (userModulePort.existsByPhone(Phone.of(req.phone()))) {
            log.warn("[手机号注册] 手机号已存在: phone={}", req.phone());
            throw new DomainException(AuthResultCode.PHONE_ALREADY_EXISTS);
        }

        // 4. 创建用户
        Phone phone = Phone.of(req.phone());
        Password password = Password.of(req.password());
        CreateUserCommand createCommand = CreateUserCommand.builder()
                .phone(phone)
                .password(password)
                .build();

        var userId = userModulePort.createUser(createCommand);
        log.info("[手机号注册] 用户创建成功: userId={}", userId.value());

        // 5. 查询完整用户信息
        Optional<UserDTO> userOpt = userModulePort.findById(userId);
        if (userOpt.isEmpty()) {
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }

        return userOpt.get();
    }

    @Override
    protected RegisterType getSupportedRegisterType() {
        return RegisterType.PHONE_PASSWORD;
    }
}
