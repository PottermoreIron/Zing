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
import com.pot.auth.interfaces.dto.auth.EmailCodeRegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 邮箱验证码注册策略
 *
 * <p>使用邮箱+验证码注册（无密码）
 *
 * @author yecao
 * @since 2025-11-18
 */
@Slf4j
@Component
public class EmailCodeRegisterStrategy extends AbstractRegisterStrategyImpl<EmailCodeRegisterRequest> {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;

    public EmailCodeRegisterStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory,
            VerificationCodeService verificationCodeService
    ) {
        super(jwtTokenService);
        this.userModulePortFactory = userModulePortFactory;
        this.verificationCodeService = verificationCodeService;
    }

    @Override
    protected void validateRequest(EmailCodeRegisterRequest request) {
        // Jakarta Validation已在Controller层完成，这里可以添加额外的业务验证
    }

    @Override
    protected UserDTO doRegister(EmailCodeRegisterRequest request, LoginContext loginContext) {
        log.info("[邮箱验证码注册] 开始注册: email={}", request.email());

        // 1. 验证验证码
        boolean codeValid = verificationCodeService.verifyCode(
                request.email(),
                VerificationCode.of(request.verificationCode())
        );
        if (!codeValid) {
            log.warn("[邮箱验证码注册] 验证码无效: email={}", request.email());
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }

        // 2. 获取用户模块适配器
        UserDomain userDomain = request.userDomain();
        UserModulePort userModulePort = userModulePortFactory.getPort(userDomain);

        // 3. 检查邮箱是否已存在
        if (userModulePort.existsByEmail(Email.of(request.email()))) {
            log.warn("[邮箱验证码注册] 邮箱已存在: email={}", request.email());
            throw new DomainException(AuthResultCode.EMAIL_ALREADY_EXISTS);
        }

        // 4. 创建用户（无密码注册）
        Email email = Email.of(request.email());
        CreateUserCommand createCommand = CreateUserCommand.builder()
                .email(email)
                .build();

        var userId = userModulePort.createUser(createCommand);
        log.info("[邮箱验证码注册] 用户创建成功: userId={}", userId.value());

        // 5. 查询完整用户信息
        Optional<UserDTO> userOpt = userModulePort.findById(userId);
        if (userOpt.isEmpty()) {
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }

        return userOpt.get();
    }

    @Override
    protected RegisterType getSupportedRegisterType() {
        return RegisterType.EMAIL_CODE;
    }
}
