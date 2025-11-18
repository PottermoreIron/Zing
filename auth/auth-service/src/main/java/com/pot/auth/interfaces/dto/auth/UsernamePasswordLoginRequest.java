package com.pot.auth.interfaces.dto.auth;

import com.pot.auth.domain.validation.annotations.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户名密码登录请求
 *
 * @author yecao
 * @since 2025-11-18
 */
public record UsernamePasswordLoginRequest(
        @NotBlank(message = "登录类型不能为空")
        String loginType,

        @NotBlank(message = "用户名不能为空")
        @Size(min = 1, max = 30, message = "用户名长度必须在1-30个字符之间")
        String username,

        @NotBlank(message = "密码不能为空")
        @ValidPassword(message = "密码必须包含大小写字母、数字、特殊字符，且长度在8-16个字符之间")
        String password,

        @Size(max = 20, message = "用户域长度不能超过20个字符")
        String userDomain
) implements LoginRequest {
}

