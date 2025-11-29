package com.pot.auth.domain.strategy.register;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.context.RegistrationContext;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.domain.strategy.AbstractRegisterStrategyImpl;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.domain.validation.handler.RegistrationParameterValidator;
import com.pot.auth.interfaces.dto.register.UsernamePasswordRegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 用户名密码注册策略（重构版）
 *
 * <p>
 * 最简单的注册方式，不需要验证码
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Component
public class UsernamePasswordRegisterStrategy extends AbstractRegisterStrategyImpl<UsernamePasswordRegisterRequest> {

    private final UserModulePortFactory userModulePortFactory;

    public UsernamePasswordRegisterStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory) {
        super(jwtTokenService, createValidationChain());
        this.userModulePortFactory = userModulePortFactory;
    }

    private static ValidationChain<RegistrationContext> createValidationChain() {
        ValidationChain<RegistrationContext> chain = new ValidationChain<>();
        chain.addHandler(new RegistrationParameterValidator());
        return chain;
    }

    @Override
    protected void validateCredential(RegistrationContext context) {
        UsernamePasswordRegisterRequest request = (UsernamePasswordRegisterRequest) context.request();

        log.debug("[用户名注册] 开始验证凭证: username={}", request.username());

        // 用户名密码注册无需额外验证（密码强度已在DTO验证层完成）
        // 如需添加特殊验证（如密码黑名单检查），可在此处添加

        log.debug("[用户名注册] 凭证验证通过: username={}", request.username());
    }

    @Override
    protected void beforeRegister(RegistrationContext context) {
        UsernamePasswordRegisterRequest request = (UsernamePasswordRegisterRequest) context.request();

        log.debug("[用户名注册] 注册前置检查: username={}", request.username());

        // 检查用户名是否已存在
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        if (userModulePort.existsByUsername(request.username())) {
            log.warn("[用户名注册] 用户名已存在: username={}", request.username());
            throw new DomainException(AuthResultCode.USERNAME_ALREADY_EXISTS);
        }
    }

    @Override
    protected UserDTO createUser(RegistrationContext context) {
        UsernamePasswordRegisterRequest request = (UsernamePasswordRegisterRequest) context.request();

        log.info("[用户名注册] 创建用户: username={}", request.username());

        // 创建用户
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        Password password = Password.of(request.password());

        CreateUserCommand createCommand = CreateUserCommand.builder()
                .username(request.username())
                .password(password)
                .build();

        var userId = userModulePort.createUser(createCommand);
        log.info("[用户名注册] 用户创建成功: userId={}", userId.value());

        // 查询完整用户信息
        Optional<UserDTO> userOpt = userModulePort.findById(userId);
        if (userOpt.isEmpty()) {
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }

        return userOpt.get();
    }

    @Override
    protected RegisterType getSupportedRegisterType() {
        return RegisterType.USERNAME_PASSWORD;
    }
}
