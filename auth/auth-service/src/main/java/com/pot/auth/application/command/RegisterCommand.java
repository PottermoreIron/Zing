package com.pot.auth.application.command;

import com.pot.auth.domain.registration.valueobject.RegistrationType;
import com.pot.auth.domain.shared.valueobject.UserDomain;

import java.util.Map;

/**
 * 注册命令
 *
 * <p>支持三种注册方式：
 * <ul>
 *   <li>用户名-密码注册：最简单，不需要验证码</li>
 *   <li>邮箱-密码注册：需要邮箱验证码，系统自动生成用户名</li>
 *   <li>手机号-密码注册：需要手机验证码，系统自动生成用户名</li>
 * </ul>
 *
 * @author yecao
 * @since 2025-11-10
 */
public record RegisterCommand(
        RegistrationType registrationType,
        UserDomain userDomain,
        String username,        // 可选，仅用户名注册时必填
        String email,          // 可选，仅邮箱注册时必填
        String phone,          // 可选，仅手机号注册时必填
        String password,       // 必填
        String verificationCode, // 可选，邮箱和手机号注册时必填
        String ipAddress,
        String userAgent,
        Map<String, Object> extendAttributes
) {

    /**
     * 用户名注册命令
     * <p>最简单的注册方式，不需要验证码
     */
    public static RegisterCommand forUsername(
            UserDomain userDomain,
            String username,
            String password,
            String ipAddress,
            String userAgent
    ) {
        return new RegisterCommand(
                RegistrationType.USERNAME,
                userDomain,
                username,
                null,
                null,
                password,
                null, // 用户名注册不需要验证码
                ipAddress,
                userAgent,
                Map.of()
        );
    }

    /**
     * 邮箱注册命令
     * <p>使用邮箱作为主要身份标识，系统自动生成用户名
     */
    public static RegisterCommand forEmail(
            UserDomain userDomain,
            String email,
            String password,
            String verificationCode,
            String ipAddress,
            String userAgent
    ) {
        return new RegisterCommand(
                RegistrationType.EMAIL,
                userDomain,
                null, // 邮箱注册不需要username，系统会自动生成
                email,
                null,
                password,
                verificationCode,
                ipAddress,
                userAgent,
                Map.of()
        );
    }

    /**
     * 手机号注册命令
     * <p>使用手机号作为主要身份标识，系统自动生成用户名
     */
    public static RegisterCommand forPhone(
            UserDomain userDomain,
            String phone,
            String password,
            String verificationCode,
            String ipAddress,
            String userAgent
    ) {
        return new RegisterCommand(
                RegistrationType.PHONE,
                userDomain,
                null, // 手机号注册不需要username，系统会自动生成
                null,
                phone,
                password,
                verificationCode,
                ipAddress,
                userAgent,
                Map.of()
        );
    }
}

