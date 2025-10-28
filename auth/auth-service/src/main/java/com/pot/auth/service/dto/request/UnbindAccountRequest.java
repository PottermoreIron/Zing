package com.pot.auth.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 解绑第三方账号请求
 *
 * @author Zing
 * @since 2025-10-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "解绑第三方账号请求")
public class UnbindAccountRequest {

    @NotBlank(message = "OAuth提供商不能为空")
    @Schema(description = "OAuth提供商", example = "wechat",
            allowableValues = {"wechat", "github", "google", "facebook", "twitter"})
    private String provider;

    @Schema(description = "解绑原因", example = "不再使用该账号")
    private String reason;
}

