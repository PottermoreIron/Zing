package com.pot.auth.interfaces.dto.login;

import com.pot.auth.domain.shared.valueobject.UserDomain;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 邮箱验证码登录请求DTO
 *
 * @author yecao
 * @since 2025-11-10
 */
public record EmailCodeLoginRequest(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱长度不能超过100个字符")
        String email,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^[0-9]{6}$", message = "验证码必须是6位数字")
        String verificationCode,

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

