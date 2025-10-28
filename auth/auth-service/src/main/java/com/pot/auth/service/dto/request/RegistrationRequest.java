package com.pot.auth.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 用户注册请求
 * <p>
 * 支持多种注册方式：
 * - username_password: 用户名密码注册
 * - phone_code: 手机号验证码注册
 * - email_code: 邮箱验证码注册
 *
 * @author Zing
 * @since 2025-10-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户注册请求")
public class RegistrationRequest {

    @NotBlank(message = "注册类型不能为空")
    @Pattern(regexp = "^(username_password|phone_code|email_code)$",
            message = "注册类型必须是username_password、phone_code或email_code")
    @Schema(description = "注册类型", required = true,
            allowableValues = {"username_password", "phone_code", "email_code"},
            example = "username_password")
    private String registrationType;

    @NotBlank(message = "标识符不能为空")
    @Schema(description = "标识符（用户名/手机号/邮箱）", required = true, example = "john_doe")
    private String identifier;

    @Size(min = 8, max = 32, message = "密码长度必须在8-32位之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$",
            message = "密码必须包含大小写字母和数字")
    @Schema(description = "密码（用户名密码注册时必填）", example = "Password123!")
    private String password;

    @Size(min = 6, max = 6, message = "验证码必须是6位")
    @Schema(description = "验证码（手机号/邮箱验证码注册时必填）", example = "123456")
    private String code;

    @Size(min = 2, max = 50, message = "昵称长度必须在2-50位之间")
    @Schema(description = "昵称", example = "John Doe")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱（可选，用于接收通知）", example = "john@example.com")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号（可选）", example = "13800138000")
    private String phone;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Schema(description = "客户端ID", example = "web")
    private String clientId;

    @Schema(description = "设备信息", example = "Chrome 120.0 on Windows 10")
    private String deviceInfo;

    @Schema(description = "是否同意用户协议", example = "true")
    private Boolean agreedToTerms;

    @Schema(description = "邀请码", example = "INV123456")
    private String invitationCode;
}

