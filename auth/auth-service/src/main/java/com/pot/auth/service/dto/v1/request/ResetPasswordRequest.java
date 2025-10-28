package com.pot.auth.service.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author: Pot
 * @created: 2025/10/25 23:52
 * @description: 重置密码请求
 */
@Schema(description = "重置密码请求")
@Data
public class ResetPasswordRequest {
    @NotBlank(message = "凭证不能为空")
    @Schema(description = "凭证（手机号或邮箱）", example = "13800138000")
    private String credential;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "验证码必须是6位数字")
    @Schema(description = "验证码", example = "123456")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8-20位之间")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "密码必须包含大写字母、小写字母和数字"
    )
    @Schema(description = "新密码", example = "NewPassword123!")
    private String newPassword;
}
