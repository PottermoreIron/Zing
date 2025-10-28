package com.pot.auth.service.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author: Pot
 * @created: 2025/10/25 23:52
 * @description: 修改密码请求
 */
@Schema(description = "修改密码请求")
@Data
public class ChangePasswordRequest {
    @NotBlank(message = "旧密码不能为空")
    @Schema(description = "旧密码", example = "OldPassword123!")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8-20位之间")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "密码必须包含大写字母、小写字母和数字"
    )
    @Schema(description = "新密码", example = "NewPassword123!")
    private String newPassword;
}
