package com.pot.auth.service.dto.v1.request;

import com.pot.auth.service.dto.v1.session.AuthSession;
import com.pot.auth.service.service.v1.AuthenticationService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OAuth2授权码授权请求
 * <p>
 * 对应OAuth 2.0的Authorization Code Grant
 * <p>
 * 支持的OAuth2提供商：
 * - GitHub
 * - Google
 * - Facebook
 * - Twitter
 * - LinkedIn
 * 等
 *
 * @author Pot
 * @since 2025-10-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "OAuth2授权码授权请求")
public class AuthorizationCodeGrantRequest extends CreateSessionRequest {

    /**
     * OAuth2提供商
     */
    @NotBlank(message = "OAuth2提供商不能为空")
    @Schema(
            description = "OAuth2提供商",
            example = "github",
            allowableValues = {"github", "google", "facebook", "twitter", "linkedin"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String provider;

    /**
     * OAuth2授权码
     */
    @NotBlank(message = "授权码不能为空")
    @Schema(
            description = "OAuth2授权码",
            example = "4/0AY0e-g7...",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String code;

    /**
     * State参数（CSRF防护）
     */
    @NotBlank(message = "state参数不能为空")
    @Schema(
            description = "State参数（用于CSRF防护）",
            example = "random_state_string",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String state;

    /**
     * 回调URI
     * 可选，用于验证
     */
    @Schema(
            description = "回调URI",
            example = "https://example.com/oauth2/callback"
    )
    private String redirectUri;

    /**
     * 是否自动绑定
     * true: 如果OAuth账号未绑定，自动创建新用户并绑定
     * false: 如果OAuth账号未绑定，抛出异常
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

