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
import com.pot.auth.domain.strategy.AbstractRegisterStrategyImpl;
import com.pot.auth.interfaces.dto.auth.PhoneCodeRegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 手机号验证码注册策略
 *
 * <p>使用手机号+验证码注册（无密码）
 *
 * @author yecao
 * @since 2025-11-18
 */
@Slf4j
@Component
public class PhoneCodeRegisterStrategy extends AbstractRegisterStrategyImpl<PhoneCodeRegisterRequest> {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;

    public PhoneCodeRegisterStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory,
            VerificationCodeService verificationCodeService
    ) {
        super(jwtTokenService);
        this.userModulePortFactory = userModulePortFactory;
        this.verificationCodeService = verificationCodeService;
    }

    @Override
    protected void validateRequest(PhoneCodeRegisterRequest request) {
        // Jakarta Validation已在Controller层完成，这里可以添加额外的业务验证
    }

    @Override
    protected UserDTO doRegister(PhoneCodeRegisterRequest request, LoginContext loginContext) {
        log.info("[手机号验证码注册] 开始注册: phone={}", request.phone());

        // 1. 验证验证码
        boolean codeValid = verificationCodeService.verifyCode(
                request.phone(),
                VerificationCode.of(request.verificationCode())
        );
        if (!codeValid) {
            log.warn("[手机号验证码注册] 验证码无效: phone={}", request.phone());
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }

        // 2. 获取用户模块适配器
        UserDomain userDomain = request.userDomain();
        UserModulePort userModulePort = userModulePortFactory.getPort(userDomain);

        // 3. 检查手机号是否已存在
        if (userModulePort.existsByPhone(Phone.of(request.phone()))) {
            log.warn("[手机号验证码注册] 手机号已存在: phone={}", request.phone());
            throw new DomainException(AuthResultCode.PHONE_ALREADY_EXISTS);
        }

        // 4. 创建用户（无密码注册）
        Phone phone = Phone.of(request.phone());
        CreateUserCommand createCommand = CreateUserCommand.builder()
                .phone(phone)
                .build();

        var userId = userModulePort.createUser(createCommand);
        log.info("[手机号验证码注册] 用户创建成功: userId={}", userId.value());

        // 5. 查询完整用户信息
        Optional<UserDTO> userOpt = userModulePort.findById(userId);
        if (userOpt.isEmpty()) {
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }

        return userOpt.get();
    }

    @Override
    protected RegisterType getSupportedRegisterType() {
        return RegisterType.PHONE_CODE;
    }
}
