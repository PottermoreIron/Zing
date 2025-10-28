package com.pot.auth.service.dto.v1.request;

import com.pot.auth.service.dto.v1.session.AuthSession;
import com.pot.auth.service.service.v1.AuthenticationService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 微信扫码授权请求
 * <p>
 * 微信开放平台扫码登录
 * 单独作为一个授权类型，因为微信有特殊的流程和参数
 *
 * @author Pot
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "微信扫码授权请求")
public class WeChatQrCodeGrantRequest extends CreateSessionRequest {

    /**
     * 微信授权码
     */
    @NotBlank(message = "授权码不能为空")
    @Schema(
            description = "微信授权码",
            example = "wx_code_xxx",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String code;

    /**
     * State参数（CSRF防护）
     */
    @NotBlank(message = "state参数不能为空")
    @Schema(
            description = "State参数",
            example = "random_state_string",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String state;

    /**
     * 是否自动绑定
     */
    @Schema(
            description = "是否自动绑定",
            example = "true",
            defaultValue = "true"
    )
    private Boolean autoBind = true;

    @Override
    public AuthSession authenticate(AuthenticationService authService) {
        return authService.authenticate(this);
    }
}