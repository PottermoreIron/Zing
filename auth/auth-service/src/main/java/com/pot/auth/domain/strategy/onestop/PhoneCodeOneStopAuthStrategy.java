package com.pot.auth.domain.strategy.onestop;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.context.OneStopAuthContext;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.domain.shared.valueobject.Phone;
import com.pot.auth.domain.shared.valueobject.VerificationCode;
import com.pot.auth.domain.strategy.AbstractOneStopAuthStrategyImpl;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.interfaces.dto.onestop.PhoneCodeAuthRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 手机号验证码一键认证策略
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Component
public class PhoneCodeOneStopAuthStrategy
        extends AbstractOneStopAuthStrategyImpl<PhoneCodeAuthRequest> {

    private final UserModulePortFactory userModulePortFactory;
    private final VerificationCodeService verificationCodeService;

    public PhoneCodeOneStopAuthStrategy(
            JwtTokenService jwtTokenService,
            UserModulePortFactory userModulePortFactory,
            VerificationCodeService verificationCodeService,
            UserDefaultsGenerator userDefaultsGenerator) {
        super(jwtTokenService, createValidationChain(), userDefaultsGenerator);
        this.userModulePortFactory = userModulePortFactory;
        this.verificationCodeService = verificationCodeService;
    }

    private static ValidationChain<OneStopAuthContext> createValidationChain() {
        return new ValidationChain<>();
    }

    @Override
    protected UserDTO findUser(OneStopAuthContext context) {
        PhoneCodeAuthRequest request = (PhoneCodeAuthRequest) context.request();
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        return userModulePort.findByPhone(request.phone()).orElse(null);
    }

    @Override
    protected void validateCredentialForLogin(OneStopAuthContext context, UserDTO user) {
        PhoneCodeAuthRequest request = (PhoneCodeAuthRequest) context.request();

        // 验证验证码
        boolean codeValid = verificationCodeService.verifyCode(
                request.phone(),
                VerificationCode.of(request.verificationCode()));

        if (!codeValid) {
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }
    }

    @Override
    protected void validateCredentialForRegister(OneStopAuthContext context) {
        // 同登录验证
        validateCredentialForLogin(context, null);
    }

    @Override
    protected UserDTO createUserWithDefaults(OneStopAuthContext context) {
        PhoneCodeAuthRequest request = (PhoneCodeAuthRequest) context.request();

        // 生成默认值
        String username = userDefaultsGenerator.generateUsernameFromPhone(request.phone());
        String password = userDefaultsGenerator.generateRandomPassword();
        String avatarUrl = userDefaultsGenerator.getDefaultAvatarUrl();

        // 创建用户
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        CreateUserCommand command = CreateUserCommand.builder()
                .phone(Phone.of(request.phone()))
                .password(Password.of(password))
                .username(username)
                .avatarUrl(avatarUrl)
                .build();

        var userId = userModulePort.createUser(command);

        return userModulePort.findById(userId)
                .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
    }

    @Override
    protected void afterRegister(UserDTO user, OneStopAuthContext context) {
        PhoneCodeAuthRequest request = (PhoneCodeAuthRequest) context.request();
        verificationCodeService.deleteCode(request.phone());
    }

    @Override
    protected void afterLogin(UserDTO user, com.pot.auth.domain.authentication.entity.AuthenticationResult result,
                              OneStopAuthContext context) {
        PhoneCodeAuthRequest request = (PhoneCodeAuthRequest) context.request();
        verificationCodeService.deleteCode(request.phone());
    }

    @Override
    public AuthType getSupportedAuthType() {
        return AuthType.PHONE_CODE;
    }
}
