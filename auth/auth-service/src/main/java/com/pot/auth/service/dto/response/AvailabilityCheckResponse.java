package com.pot.auth.service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 可用性检查响应
 *
 * @author Zing
 * @since 2025-10-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "可用性检查响应")
public class AvailabilityCheckResponse {

    @Schema(description = "是否可用", example = "true")
    private Boolean available;

    @Schema(description = "检查类型", example = "username")
    private String type;

    @Schema(description = "检查的值", example = "john_doe")
    private String value;

    @Schema(description = "不可用原因（当available=false时）", example = "该用户名已被注册")
    private String reason;

    @Schema(description = "建议（当available=false时）", example = "试试 john_doe_2023")
    private String suggestion;
}

