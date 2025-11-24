package com.pot.auth.domain.strategy.login;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.LoginContext;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.strategy.AbstractLoginStrategyImpl;
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
public class PhoneCodeLoginStrategy extends AbstractLoginStrategyImpl<PhoneCodeLoginRequest> {

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
    protected void validateRequest(PhoneCodeLoginRequest request) {
        // Jakarta Validation已在Controller层完成，这里可以添加额外的业务验证
    }

    @Override
    protected UserDTO doLogin(PhoneCodeLoginRequest request, LoginContext loginContext) {
        log.info("[手机号验证码登录] 开始登录: phone={}", request.phone());

        // 1. 验证验证码
        boolean codeValid = verificationCodeService.verifyCode(request.phone(), request.verificationCode());
        if (!codeValid) {
            log.warn("[手机号验证码登录] 验证码验证失败: phone={}", request.phone());
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }

        // 2. 获取用户模块适配器
        UserDomain userDomain = request.userDomain();
        UserModulePort userModulePort = userModulePortFactory.getPort(userDomain);

        // 3. 根据手机号查找用户
        Optional<UserDTO> userOpt = userModulePort.findByPhone(request.phone());

        if (userOpt.isEmpty()) {
            log.warn("[手机号验证码登录] 用户不存在: phone={}", request.phone());
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }

        UserDTO user = userOpt.get();

        // 4. 检查用户状态
        validateUserStatus(user);

        log.info("[手机号验证码登录] 登录成功: userId={}", user.userId());
        return user;
    }

    @Override
    protected LoginType getSupportedLoginType() {
        return LoginType.PHONE_CODE;
    }

    @Override
    public boolean supports(LoginType loginType) {
        return LoginType.PHONE_CODE.equals(loginType);
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
