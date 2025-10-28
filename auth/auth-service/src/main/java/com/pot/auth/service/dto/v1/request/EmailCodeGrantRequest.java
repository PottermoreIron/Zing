package com.pot.auth.service.dto.v1.request;

import com.pot.auth.service.dto.v1.session.AuthSession;
import com.pot.auth.service.service.v1.AuthenticationService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 邮箱验证码授权请求
 * <p>
 * 自定义扩展的授权方式，用于邮箱快捷登录
 *
 * @author Pot
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "邮箱验证码授权请求")
public class EmailCodeGrantRequest extends CreateSessionRequest {

    /**
     * 邮箱地址
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(
            description = "邮箱地址",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "验证码必须是6位数字")
    @Schema(
            description = "验证码",
            example = "123456",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String code;

    /**
     * 是否自动注册
     */
    @Schema(
            description = "是否自动注册",
            example = "true",
            defaultValue = "true"
    )
    private Boolean autoRegister = true;

    @Override
    public AuthSession authenticate(AuthenticationService authService) {
        return authService.authenticate(this);
    }
}

