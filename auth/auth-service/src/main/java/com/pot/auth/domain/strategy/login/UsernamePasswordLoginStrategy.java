package com.pot.auth.domain.strategy.login;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.context.AuthenticationContext;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.strategy.AbstractLoginStrategyImpl;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.domain.validation.handler.AuthenticationParameterValidator;
import com.pot.auth.interfaces.dto.auth.UsernamePasswordLoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 用户名密码登录策略（重构版）
 *
 * <p>
 * 通过用户名和密码进行登录认证
 * <p>
 * 新版特性：
 * <ul>
 * <li>使用 AuthenticationContext 封装上下文</li>
 * <li>集成责任链校验</li>
 * <li>凭证验证和用户获取分离</li>
 * <li>支持前置/后置钩子扩展</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Component
public class UsernamePasswordLoginStrategy extends AbstractLoginStrategyImpl<UsernamePasswordLoginRequest> {

    private final UserModulePortFactory userModulePortFactory;

    public UsernamePasswordLoginStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory) {
        super(jwtTokenService, createValidationChain());
        this.userModulePortFactory = userModulePortFactory;
    }

    /**
     * 创建校验链
     */
    private static ValidationChain<AuthenticationContext> createValidationChain() {
        ValidationChain<AuthenticationContext> chain = new ValidationChain<>();
        chain.addHandler(new AuthenticationParameterValidator());
        // 可以继续添加其他校验器：风控、频率限制等
        return chain;
    }

    @Override
    protected void validateCredential(AuthenticationContext context) {
        UsernamePasswordLoginRequest request = (UsernamePasswordLoginRequest) context.request();

        log.debug("[用户名登录] 开始验证凭证: username={}", request.username());

        // 获取用户模块适配器
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());

        // 验证用户名和密码
        Optional<UserDTO> userOpt = userModulePort.authenticateWithPassword(
                request.username(),
                request.password());

        if (userOpt.isEmpty()) {
            log.warn("[用户名登录] 密码验证失败: username={}", request.username());
            throw new DomainException(AuthResultCode.AUTHENTICATION_FAILED);
        }

        log.debug("[用户名登录] 凭证验证通过: username={}", request.username());
    }

    @Override
    protected UserDTO getUserInfo(AuthenticationContext context) {
        UsernamePasswordLoginRequest request = (UsernamePasswordLoginRequest) context.request();

        log.debug("[用户名登录] 获取用户信息: username={}", request.username());

        // 通过用户名查询用户（使用findByIdentifier，它支持用户名/邮箱/手机号）
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        Optional<UserDTO> userOpt = userModulePort.findByIdentifier(request.username());

        if (userOpt.isEmpty()) {
            log.error("[用户名登录] 用户不存在: username={}", request.username());
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }

        return userOpt.get();
    }

    @Override
    protected LoginType getSupportedLoginType() {
        return LoginType.USERNAME_PASSWORD;
    }
}
