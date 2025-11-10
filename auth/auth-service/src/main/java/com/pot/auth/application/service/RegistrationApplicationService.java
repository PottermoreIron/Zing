package com.pot.auth.application.service;

import com.pot.auth.application.command.RegisterCommand;
import com.pot.auth.application.dto.RegisterResponse;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.registration.entity.RegistrationRequest;
import com.pot.auth.domain.registration.service.RegistrationDomainService;
import com.pot.auth.domain.shared.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 注册应用服务
 *
 * <p>编排注册流程，协调领域服务完成业务逻辑
 *
 * @author yecao
 * @since 2025-11-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationApplicationService {

    private final RegistrationDomainService registrationDomainService;

    /**
     * 用户名注册
     *
     * <p>最简单的注册方式，不需要验证码
     *
     * @param command 注册命令
     * @return 注册响应（包含Token）
     */
    public RegisterResponse registerWithUsername(RegisterCommand command) {
        log.info("[应用服务] 用户名注册: username={}, userDomain={}",
                command.username(), command.userDomain());

        // 1. 构建值对象
        Password password = Password.of(command.password());
        IpAddress ipAddress = IpAddress.of(command.ipAddress());
        DeviceInfo deviceInfo = DeviceInfo.fromUserAgent(command.userAgent());
        LoginContext loginContext = LoginContext.of(ipAddress, deviceInfo);

        // 2. 构建注册请求
        RegistrationRequest request = RegistrationRequest.builder()
                .registrationType(command.registrationType())
                .userDomain(command.userDomain())
                .username(command.username())
                .password(password)
                .loginContext(loginContext)
                .extendAttributes(command.extendAttributes())
                .build();

        // 3. 调用领域服务注册
        AuthenticationResult result = registrationDomainService.registerWithUsername(request);

        // 4. 转换为应用层DTO
        RegisterResponse response = RegisterResponse.success(
                result.userId().value(),
                result.userDomain().name(),
                result.username(),
                result.email(),
                result.phone(),
                result.accessToken(),
                result.refreshToken(),
                result.accessTokenExpiresAt(),
                result.refreshTokenExpiresAt()
        );

        log.info("[应用服务] 用户名注册成功: userId={}", result.userId());
        return response;
    }

    /**
     * 邮箱注册
     *
     * @param command 注册命令
     * @return 注册响应（包含Token）
     */
    public RegisterResponse registerWithEmail(RegisterCommand command) {
        log.info("[应用服务] 邮箱注册: email={}, userDomain={}",
                command.email(), command.userDomain());

        // 1. 构建值对象
        Email email = Email.of(command.email());
        Password password = Password.of(command.password());
        VerificationCode code = VerificationCode.of(command.verificationCode());
        IpAddress ipAddress = IpAddress.of(command.ipAddress());
        DeviceInfo deviceInfo = DeviceInfo.fromUserAgent(command.userAgent());
        LoginContext loginContext = LoginContext.of(ipAddress, deviceInfo);

        // 2. 构建注册请求
        RegistrationRequest request = RegistrationRequest.builder()
                .registrationType(command.registrationType())
                .userDomain(command.userDomain())
                .username(command.username())
                .email(email)
                .password(password)
                .verificationCode(code)
                .loginContext(loginContext)
                .extendAttributes(command.extendAttributes())
                .build();

        // 3. 调用领域服务注册
        AuthenticationResult result = registrationDomainService.registerWithEmail(request);

        // 4. 转换为应用层DTO
        RegisterResponse response = RegisterResponse.success(
                result.userId().value(),
                result.userDomain().name(),
                result.username(),
                result.email(),
                result.phone(),
                result.accessToken(),
                result.refreshToken(),
                result.accessTokenExpiresAt(),
                result.refreshTokenExpiresAt()
        );

        log.info("[应用服务] 邮箱注册成功: userId={}", result.userId());
        return response;
    }

    /**
     * 手机号注册
     *
     * @param command 注册命令
     * @return 注册响应（包含Token）
     */
    public RegisterResponse registerWithPhone(RegisterCommand command) {
        log.info("[应用服务] 手机号注册: phone={}, userDomain={}",
                command.phone(), command.userDomain());

        // 1. 构建值对象
        Phone phoneNumber = Phone.of(command.phone());
        Password password = Password.of(command.password());
        VerificationCode code = VerificationCode.of(command.verificationCode());
        IpAddress ipAddress = IpAddress.of(command.ipAddress());
        DeviceInfo deviceInfo = DeviceInfo.fromUserAgent(command.userAgent());
        LoginContext loginContext = LoginContext.of(ipAddress, deviceInfo);

        // 2. 构建注册请求
        RegistrationRequest request = RegistrationRequest.builder()
                .registrationType(command.registrationType())
                .userDomain(command.userDomain())
                .username(command.username())
                .phone(phoneNumber)
                .password(password)
                .verificationCode(code)
                .loginContext(loginContext)
                .extendAttributes(command.extendAttributes())
                .build();

        // 3. 调用领域服务注册
        AuthenticationResult result = registrationDomainService.registerWithPhone(request);

        // 4. 转换为应用层DTO
        RegisterResponse response = RegisterResponse.success(
                result.userId().value(),
                result.userDomain().name(),
                result.username(),
                result.email(),
                result.phone(),
                result.accessToken(),
                result.refreshToken(),
                result.accessTokenExpiresAt(),
                result.refreshTokenExpiresAt()
        );

        log.info("[应用服务] 手机号注册成功: userId={}", result.userId());
        return response;
    }
}

