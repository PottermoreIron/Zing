package com.pot.auth.service.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author: Pot
 * @created: 2025/10/26 00:03
 * @description: OAuth回调请求
 */
@Schema(description = "OAuth回调请求")
@Data
public class OAuthCallbackRequest {
    @NotBlank(message = "提供商不能为空")
    @Schema(description = "提供商ID", example = "github", requiredMode = Schema.RequiredMode.REQUIRED)
    private String provider;

    @NotBlank(message = "授权码不能为空")
    @Schema(description = "授权码", example = "4/0AY0e-g7...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotBlank(message = "state不能为空")
    @Schema(description = "State参数", example = "random_state", requiredMode = Schema.RequiredMode.REQUIRED)
    private String state;
}

