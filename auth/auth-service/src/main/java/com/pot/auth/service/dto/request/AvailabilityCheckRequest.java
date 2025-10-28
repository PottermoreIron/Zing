package com.pot.auth.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 可用性检查请求
 *
 * @author Zing
 * @since 2025-10-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "可用性检查请求")
public class AvailabilityCheckRequest {

    @NotBlank(message = "检查类型不能为空")
    @Pattern(regexp = "^(username|phone|email)$", message = "检查类型必须是username、phone或email")
    @Schema(description = "检查类型", required = true,
            allowableValues = {"username", "phone", "email"},
            example = "username")
    private String type;

    @NotBlank(message = "检查值不能为空")
    @Schema(description = "要检查的值", required = true, example = "john_doe")
    private String value;
}

