package com.pot.auth.domain.strategy.login;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.LoginContext;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.strategy.AbstractLoginStrategy;
import com.pot.auth.interfaces.dto.auth.LoginRequest;
import com.pot.auth.interfaces.dto.auth.PhoneCodeLoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 手机号验证码登录策略
 *
 * <p>通过手机号和验证码进行登录认证
 *
 * @author yecao
 * @since 2025-11-19
 */
@Slf4j
@Component
public class PhoneCodeLoginStrategy extends AbstractLoginStrategy {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;

    public PhoneCodeLoginStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory,
            VerificationCodeService verificationCodeService
    ) {
        super(jwtTokenService);
        this.userModulePortFactory = userModulePortFactory;
        this.verificationCodeService = verificationCodeService;
    }

    @Override
    protected void validateRequest(LoginRequest request) {
        if (!(request instanceof PhoneCodeLoginRequest)) {
            throw new DomainException(AuthResultCode.INVALID_LOGIN_REQUEST);
        }
    }

    @Override
    protected UserDTO doLogin(LoginRequest request, LoginContext loginContext) {
        PhoneCodeLoginRequest req = (PhoneCodeLoginRequest) request;

        log.info("[手机号验证码登录] 开始登录: phone={}", req.phone());

        // 1. 验证验证码
        boolean codeValid = verificationCodeService.verifyCode(req.phone(), req.verificationCode());
        if (!codeValid) {
            log.warn("[手机号验证码登录] 验证码验证失败: phone={}", req.phone());
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }

        // 2. 获取用户模块适配器
        UserDomain userDomain = UserDomain.fromCode(req.userDomain());
        UserModulePort userModulePort = userModulePortFactory.getPort(userDomain);

        // 3. 根据手机号查找用户
        Optional<UserDTO> userOpt = userModulePort.findByPhone(req.phone());

        if (userOpt.isEmpty()) {
            log.warn("[手机号验证码登录] 用户不存在: phone={}", req.phone());
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }

        UserDTO user = userOpt.get();

        // 4. 检查用户状态
        validateUserStatus(user);

        log.info("[手机号验证码登录] 登录成功: userId={}", user.userId());
        return user;
    }

    @Override
    protected String getSupportedLoginType() {
        return "PHONE_CODE";
    }

    @Override
    public boolean supports(String loginType) {
        return "PHONE_CODE".equals(loginType);
    }

    /**
     * 验证用户状态
     */
    private void validateUserStatus(UserDTO user) {
        if ("LOCKED".equals(user.status())) {
            log.warn("[手机号验证码登录] 用户已被锁定: userId={}", user.userId());
            throw new DomainException(AuthResultCode.ACCOUNT_LOCKED);
        }

        if ("DISABLED".equals(user.status())) {
            log.warn("[手机号验证码登录] 用户已被禁用: userId={}", user.userId());
            throw new DomainException(AuthResultCode.ACCOUNT_DISABLED);
        }
    }
}

