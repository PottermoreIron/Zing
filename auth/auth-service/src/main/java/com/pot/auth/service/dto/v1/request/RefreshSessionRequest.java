package com.pot.auth.service.dto.v1.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author: Pot
 * @created: 2025/10/26 00:26
 * @description: 刷新会话请求
 */
@Schema(description = "刷新会话请求")
@Data
public class RefreshSessionRequest {
    @Schema(description = "刷新令牌", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}
