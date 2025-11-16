package com.pot.auth.interfaces.dto.register;

import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidEmail;
import com.pot.auth.domain.validation.annotations.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.pot.zing.framework.common.util.ValidationUtils.PASSWORD_REGEX;

/**
 * 邮箱密码注册请求DTO
 *
 * <p>使用邮箱作为主要身份标识进行注册
 * <ul>
 *   <li>邮箱 - 必填，作为用户唯一标识和登录凭证</li>
 *   <li>密码 - 必填，用于后续密码登录</li>
 *   <li>验证码 - 必填，验证邮箱所有权</li>
 * </ul>
 *
 * <p>注册成功后，系统会自动生成默认用户名（可在后续修改）
 *
 * @author yecao
 * @since 2025-11-10
 */
public record EmailRegisterRequest(
        @NotBlank(message = "邮箱不能为空")
        @ValidEmail(message = "邮箱格式不正确")
        String email,

        @NotBlank
        @ValidPassword(message = "密码必须包含大小写字母, 数字, 特殊字符，且长度在8-16个字符之间")
        String password,

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

