package com.pot.auth.service.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @author: Pot
 * @created: 2025/10/25 23:50
 * @description: 发送验证码请求
 */
@Schema(description = "发送验证码请求")
@Data
public class SendVerificationCodeRequest {
    @NotBlank(message = "类型不能为空")
    @Pattern(regexp = "sms|email", message = "类型必须是sms或email")
    @Schema(description = "类型", example = "sms", allowableValues = {"sms", "email"})
    private String type;

    @NotBlank(message = "接收者不能为空")
    @Schema(description = "接收者（手机号或邮箱）", example = "13800138000")
    private String recipient;

    @NotBlank(message = "用途不能为空")
    @Pattern(regexp = "login|register|reset_password|bind_phone|bind_email",
            message = "用途必须是login、register、reset_password、bind_phone或bind_email之一")
    @Schema(
            description = "用途",
            example = "login",
            allowableValues = {"login", "register", "reset_password", "bind_phone", "bind_email"}
    )
    private String purpose;
}
