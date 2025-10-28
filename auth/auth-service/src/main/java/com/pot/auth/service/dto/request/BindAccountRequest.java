package com.pot.auth.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 绑定第三方账号请求
 *
 * @author Zing
 * @since 2025-10-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "绑定第三方账号请求")
public class BindAccountRequest {

    @NotBlank(message = "OAuth提供商不能为空")
    @Schema(description = "OAuth提供商", example = "wechat", allowableValues = {"wechat", "github", "google", "facebook", "twitter"})
    private String provider;

    @NotBlank(message = "授权码不能为空")
    @Schema(description = "OAuth授权码", example = "4/0AY0e-g7...")
    private String code;

    @Schema(description = "CSRF防护状态码", example = "abc123xyz")
    private String state;

    @Schema(description = "重定向URI", example = "https://example.com/auth/callback")
    private String redirectUri;
}

