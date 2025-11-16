package com.pot.auth.interfaces.dto.login;

import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.validation.annotations.ValidIdentifier;
import com.pot.auth.domain.validation.annotations.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.pot.zing.framework.common.util.ValidationUtils.PASSWORD_REGEX;

/**
 * 密码登录请求
 *
 * @author yecao
 * @since 2025-11-10
 */
public record PasswordLoginRequest(
        @NotBlank(message = "用户标识不能为空")
        @ValidIdentifier(message = "用户标识格式不正确")
        String identifier,

        @NotBlank
        @ValidPassword(message = "密码必须包含大小写字母, 数字, 特殊字符，且长度在8-16个字符之间")
        String password,         // 密码

        @Size(max = 20, message = "用户域长度不能超过20个字符")
        String userDomain        // 用户域：MEMBER, ADMIN
) {
    public UserDomain getUserDomainEnum() {
        return userDomain != null ? UserDomain.valueOf(userDomain.toUpperCase()) : UserDomain.MEMBER;
    }
}

