package com.pot.auth.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * Token撤销请求
 *
 * @author Zing
 * @since 2025-10-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Token撤销请求")
public class TokenRevokeRequest {

    @NotBlank(message = "令牌不能为空")
    @Schema(description = "要撤销的令牌", required = true)
    private String token;

    @Pattern(regexp = "^(access_token|refresh_token)$", message = "令牌类型必须是access_token或refresh_token")
    @Schema(description = "令牌类型", required = true, allowableValues = {"access_token", "refresh_token"}, example = "access_token")
    private String tokenType;

    @Schema(description = "撤销原因", example = "用户主动登出")
    private String reason;
}

