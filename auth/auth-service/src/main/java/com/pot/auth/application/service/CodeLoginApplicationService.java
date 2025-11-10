package com.pot.auth.application.service;

import com.pot.auth.application.dto.LoginResponse;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.authentication.service.AuthenticationDomainService;
import com.pot.auth.domain.authentication.service.VerificationCodeService;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.valueobject.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 验证码登录应用服务
 *
 * <p>编排验证码登录流程
 *
 * @author yecao
 * @since 2025-11-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeLoginApplicationService {

    private final AuthenticationDomainService authenticationDomainService;
    private final VerificationCodeService verificationCodeService;
    private final UserModulePortFactory userModulePortFactory;

    /**
     * 邮箱验证码登录
     *
     * @param email      邮箱
     * @param code       验证码
     * @param userDomain 用户域
     * @param ipAddress  IP地址
     * @param userAgent  User-Agent
     * @return 登录响应
     */
    public LoginResponse loginWithEmailCode(
            String email,
            String code,
            UserDomain userDomain,
            String ipAddress,
            String userAgent
    ) {
        log.info("[应用服务] 邮箱验证码登录: email={}, userDomain={}", email, userDomain);

        // 1. 构建值对象
        Email emailObj = Email.of(email);
        VerificationCode verificationCode = VerificationCode.of(code);
        IpAddress ip = IpAddress.of(ipAddress);
        DeviceInfo deviceInfo = DeviceInfo.fromUserAgent(userAgent);
        LoginContext loginContext = LoginContext.of(ip, deviceInfo);

        // 2. 验证验证码
        boolean isValid = verificationCodeService.verifyCode(email, verificationCode);
        if (!isValid) {
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }

        // 3. 获取用户模块适配器
        UserModulePort userModulePort = userModulePortFactory.getPort(userDomain);

        // 4. 查询用户
        Optional<UserDTO> userOpt = userModulePort.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }
        UserDTO user = userOpt.get();

        // 5. 检查账户状态
        if ("suspended".equals(user.status()) || "inactive".equals(user.status())) {
            throw new DomainException(AuthResultCode.ACCOUNT_DISABLED);
        }

        // 6. 删除已使用的验证码
        verificationCodeService.deleteCode(email);

        // 7. 生成认证结果
        AuthenticationResult result = authenticationDomainService.buildAuthenticationResult(
                user, userDomain, loginContext
        );

        // 8. 转换为应用层DTO
        LoginResponse response = new LoginResponse(
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

        log.info("[应用服务] 邮箱验证码登录成功: userId={}", result.userId());
        return response;
    }

    /**
     * 手机验证码登录
     *
     * @param phone      手机号
     * @param code       验证码
     * @param userDomain 用户域
     * @param ipAddress  IP地址
     * @param userAgent  User-Agent
     * @return 登录响应
     */
    public LoginResponse loginWithPhoneCode(
            String phone,
            String code,
            UserDomain userDomain,
            String ipAddress,
            String userAgent
    ) {
        log.info("[应用服务] 手机验证码登录: phone={}, userDomain={}", phone, userDomain);

        // 1. 构建值对象
        Phone phoneNumber = Phone.of(phone);
        VerificationCode verificationCode = VerificationCode.of(code);
        IpAddress ip = IpAddress.of(ipAddress);
        DeviceInfo deviceInfo = DeviceInfo.fromUserAgent(userAgent);
        LoginContext loginContext = LoginContext.of(ip, deviceInfo);

        // 2. 验证验证码
        boolean isValid = verificationCodeService.verifyCode(phone, verificationCode);
        if (!isValid) {
            throw new DomainException(AuthResultCode.VERIFICATION_CODE_INVALID);
        }

        // 3. 获取用户模块适配器
        UserModulePort userModulePort = userModulePortFactory.getPort(userDomain);

        // 4. 查询��户
        Optional<UserDTO> userOpt = userModulePort.findByPhone(phone);
        if (userOpt.isEmpty()) {
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }
        UserDTO user = userOpt.get();

        // 5. 检查账户状态
        if ("suspended".equals(user.status()) || "inactive".equals(user.status())) {
            throw new DomainException(AuthResultCode.ACCOUNT_DISABLED);
        }

        // 6. 删除已使用的验证码
        verificationCodeService.deleteCode(phone);

        // 7. 生成认证结果
        AuthenticationResult result = authenticationDomainService.buildAuthenticationResult(
                user, userDomain, loginContext
        );

        // 8. 转换为应用层DTO
        LoginResponse response = new LoginResponse(
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

        log.info("[应用服务] 手机验证码登录成功: userId={}", result.userId());
        return response;
    }
}

