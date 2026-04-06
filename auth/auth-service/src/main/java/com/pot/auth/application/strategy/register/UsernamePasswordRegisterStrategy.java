package com.pot.auth.application.strategy.register;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.application.context.RegistrationContext;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.application.strategy.AbstractRegisterStrategyImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 昵称密码注册策略
 *
 * @author pot
 */
@Slf4j
@Component
public class UsernamePasswordRegisterStrategy extends AbstractRegisterStrategyImpl {

    private final UserModulePortFactory userModulePortFactory;

    public UsernamePasswordRegisterStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory) {
        super(jwtTokenService);
        this.userModulePortFactory = userModulePortFactory;
    }

    @Override
    protected void validateCredential(RegistrationContext context) {
        // 昵称密码注册无需额外凭证验证，密码强度已在 DTO 层校验
    }

    @Override
    protected void beforeRegister(RegistrationContext context) {
        var request = context.request();
        UserModulePort port = userModulePortFactory.getPort(request.userDomain());
        if (port.existsByNickname(request.nickname())) {
            throw new DomainException(AuthResultCode.USERNAME_ALREADY_EXISTS);
        }
    }

    @Override
    protected UserDTO createUser(RegistrationContext context) {
        var request = context.request();
        UserModulePort port = userModulePortFactory.getPort(request.userDomain());

        var userId = port.createUser(CreateUserCommand.builder()
                .username(request.nickname())
                .password(Password.of(request.password()))
                .build());

        return port.findById(userId)
                .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
    }

    @Override
    public RegisterType getSupportedRegisterType() {
        return RegisterType.USERNAME_PASSWORD;
    }
}
