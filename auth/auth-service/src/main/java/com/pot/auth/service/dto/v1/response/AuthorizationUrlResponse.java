package com.pot.auth.service.dto.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/10/26 00:05
 * @description: 授权URL响应
 */
@Schema(description = "授权URL响应")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthorizationUrlResponse {
    @Schema(description = "授权URL", example = "https://github.com/login/oauth/authorize?...")
    private String authorizationUrl;

    @Schema(description = "State参数（用于CSRF防护）", example = "random_state_xxx")
    private String state;

    @Schema(description = "提供商ID", example = "github")
    private String provider;

    @Schema(description = "过期时间（秒）", example = "600")
    private Integer expiresIn;
}
