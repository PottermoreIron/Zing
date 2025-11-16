package com.pot.auth.interfaces.dto.register;

import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidPassword;
import com.pot.auth.domain.validation.annotations.ValidUsername;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.pot.zing.framework.common.util.ValidationUtils.PASSWORD_REGEX;

/**
 * 用户名密码注册请求DTO
 *
 * <p>使用用户名作为主要身份标识进行注册
 * <ul>
 *   <li>用户名 - 必填，作为用户唯一标识和登录凭证</li>
 *   <li>密码 - 必填，用于后续密码登录</li>
 * </ul>
 *
 * <p>这是最简单的注册方式，不需要验证码，适合快速注册场景
 * <p>用户可在注册后绑定邮箱或手机号
 *
 * @author yecao
 * @since 2025-11-10
 */
public record UsernameRegisterRequest(
        @NotBlank
        @ValidUsername(message = "用户名只允许中文、字母、数字、下划线，连字符，长度在1-30个字符之间")
        String username,

        @NotBlank
        @ValidPassword(message = "密码必须包含大小写字母, 数字, 特殊字符，且长度在8-16个字符之间")
        String password,

        @Size(max = 20, message = "用户域长度不能超过20个字符")
        String userDomain  // "MEMBER" 或 "ADMIN"
) {

    /**
     * 获取用户域枚举
     */
    public UserDomain getUserDomainEnum() {
        if (userDomain == null || userDomain.isBlank()) {
            return UserDomain.MEMBER; // 默认为会员域
        }
        try {
            return UserDomain.valueOf(userDomain.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserDomain.MEMBER;
        }
    }
}

