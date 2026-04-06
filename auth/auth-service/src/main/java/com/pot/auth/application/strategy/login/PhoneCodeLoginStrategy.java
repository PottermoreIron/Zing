package com.pot.auth.application.strategy.login;

import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.application.context.AuthenticationContext;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.LoginType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.application.strategy.AbstractLoginStrategyImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 手机验证码登录策略
 *
 * @author pot
 */
@Slf4j
@Component
public class PhoneCodeLoginStrategy extends AbstractLoginStrategyImpl {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;

    public PhoneCodeLoginStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory,
            VerificationCodeService verificationCodeService) {
        super(jwtTokenService);
        this.userModulePortFactory = userModulePortFactory;
        this.verificationCodeService = verificationCodeService;
    }

    @Override
    protected void validateCredential(AuthenticationContext context) {
        var request = context.request();
        log.debug("[手机验证码登录] 验证凭证: phone={}", request.phone());

        if (!verificationCodeService.verifyCode(request.phone(), request.verificationCode())) {
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }
    }

    @Override
    protected UserDTO getUserInfo(AuthenticationContext context) {
        var request = context.request();
        return userModulePortFactory.getPort(request.userDomain())
                .findByPhone(request.phone())
                .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
    }

    @Override
    public LoginType getSupportedLoginType() {
        return LoginType.PHONE_CODE;
    }
}
