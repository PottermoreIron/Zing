package com.pot.auth.interfaces.dto.login;

import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidPhone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.pot.zing.framework.common.util.ValidationUtils.PHONE_REGEX;

/**
 * 手机验证码登录请求DTO
 *
 * @author yecao
 * @since 2025-11-10
 */
public record PhoneCodeLoginRequest(
        @NotBlank
        @ValidPhone(message = "手机号格式不正确")
        String phone,

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
