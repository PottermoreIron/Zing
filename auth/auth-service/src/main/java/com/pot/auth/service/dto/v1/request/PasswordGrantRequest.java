package com.pot.auth.service.dto.v1.request;

import com.pot.auth.service.dto.v1.session.AuthSession;
import com.pot.auth.service.service.v1.AuthenticationService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 密码授权请求
 * <p>
 * 对应OAuth 2.0的Resource Owner Password Credentials Grant
 * <p>
 * 支持多种标识符：
 * - 用户名
 * - 邮箱
 * - 手机号
 *
 * @author Pot
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "密码授权请求")
public class PasswordGrantRequest extends CreateSessionRequest {

    /**
     * 用户名/邮箱/手机号
     * 系统自动识别类型
     */
    @NotBlank(message = "用户名/邮箱/手机号户名不能为空")
    @Schema(
            description = "用户名/邮箱/手机号（系统自动识别）",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Schema(
            description = "密码",
            example = "Password123!",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;

    /**
     * 是否记住我
     * 记住我时，refreshToken有效期更长
     */
    @Schema(description = "是否记住我", example = "true", defaultValue = "false")
    private Boolean rememberMe = false;

    @Override
    public AuthSession authenticate(AuthenticationService authService) {
        return authService.authenticate(this);
    }
}