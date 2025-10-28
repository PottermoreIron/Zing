package com.pot.auth.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * Token刷新请求
 *
 * @author Zing
 * @since 2025-10-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token刷新请求")
public class TokenRefreshRequest {

    @NotBlank(message = "刷新令牌不能为空")
    @Schema(description = "刷新令牌", required = true, example = "refresh_token_xyz789")
    private String refreshToken;

    @Schema(description = "客户端ID", example = "web")
    private String clientId;

    @Schema(description = "设备信息", example = "Chrome 120.0 on Windows 10")
    private String deviceInfo;
}

