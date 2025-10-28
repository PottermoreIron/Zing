package com.pot.auth.service.dto.v1.request;

import com.pot.auth.service.dto.v1.session.AuthSession;
import com.pot.auth.service.service.v1.AuthenticationService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static com.pot.zing.framework.common.util.ValidationUtils.PHONE_REGEX;

/**
 * 短信验证码授权请求
 * <p>
 * 自定义扩展的授权方式，用于手机号快捷登录
 * <p>
 * 特点：
 * 1. 无需密码
 * 2. 验证码有效期短（如5分钟）
 * 3. 支持一键登录/注册
 *
 * @author Pot
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "短信验证码授权请求")
public class SmsCodeGrantRequest extends CreateSessionRequest {

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = PHONE_REGEX, message = "手机号格式不正确")
    @Schema(
            description = "手机号",
            example = "13800138000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String phone;

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
     * true: 如果用户不存在，自动注册
     * false: 如果用户不存在，抛出异常
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

