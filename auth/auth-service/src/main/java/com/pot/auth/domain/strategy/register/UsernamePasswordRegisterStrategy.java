package com.pot.auth.domain.strategy.register;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.RegisterType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.LoginContext;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.strategy.AbstractRegisterStrategyImpl;
import com.pot.auth.interfaces.dto.auth.UsernamePasswordRegisterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 用户名密码注册策略
 *
 * <p>最简单的注册方式，不需要验证码
 *
 * @author yecao
 * @since 2025-11-18
 */
@Slf4j
@Component
public class UsernamePasswordRegisterStrategy extends AbstractRegisterStrategyImpl<UsernamePasswordRegisterRequest> {

    private final UserModulePortFactory userModulePortFactory;

    public UsernamePasswordRegisterStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory
    ) {
        super(jwtTokenService);
        this.userModulePortFactory = userModulePortFactory;
    }

    @Override
    protected void validateRequest(UsernamePasswordRegisterRequest request) {
        // Jakarta Validation已在Controller层完成，这里可以添加额外的业务验证
    }

    @Override
    protected UserDTO doRegister(UsernamePasswordRegisterRequest request, LoginContext loginContext) {
        log.info("[用户名注册] 开始注册: username={}", request.username());

        // 1. 获取用户模块适配器
        UserDomain userDomain = request.userDomain();
        UserModulePort userModulePort = userModulePortFactory.getPort(userDomain);

        // 2. 检查用户名是否已存在
        if (userModulePort.existsByUsername(request.username())) {
            log.warn("[用户名注册] 用户名已存在: username={}", request.username());
            throw new DomainException(AuthResultCode.USERNAME_ALREADY_EXISTS);
        }

        // 3. 创建用户
        Password password = Password.of(request.password());
        CreateUserCommand createCommand = CreateUserCommand.builder()
                .username(request.username())
                .password(password)
                .build();

        var userId = userModulePort.createUser(createCommand);
        log.info("[用户名注册] 用户创建成功: userId={}", userId.value());

        // 4. 查询完整用户信息
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
