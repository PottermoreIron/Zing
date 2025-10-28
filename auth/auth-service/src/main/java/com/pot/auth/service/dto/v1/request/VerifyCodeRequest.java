package com.pot.auth.service.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @author: Pot
 * @created: 2025/10/25 23:52
 * @description: 验证验证码请求
 */
@Schema(description = "验证验证码请求")
@Data
public class VerifyCodeRequest {
    @NotBlank(message = "类型不能为空")
    @Pattern(regexp = "sms|email", message = "类型必须是sms或email")
    @Schema(description = "类型", example = "sms")
    private String type;

    @NotBlank(message = "接收者不能为空")
    @Schema(description = "接收者", example = "13800138000")
    private String recipient;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "验证码必须是6位数字")
    @Schema(description = "验证码", example = "123456")
    private String code;
}
