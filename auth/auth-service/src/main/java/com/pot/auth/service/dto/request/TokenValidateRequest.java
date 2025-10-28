package com.pot.auth.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * Token验证请求
 *
 * @author Zing
 * @since 2025-10-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token验证请求")
public class TokenValidateRequest {

    @NotBlank(message = "令牌不能为空")
    @Schema(description = "要验证的令牌", required = true)
    private String token;

    @Schema(description = "是否返回详细信息", example = "true")
    private Boolean includeDetails;
}

