package com.pot.auth.service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Token验证响应
 *
 * @author Zing
 * @since 2025-10-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Token验证响应")
public class TokenValidationResponse {

    @Schema(description = "Token是否有效", example = "true")
    private Boolean valid;

    @Schema(description = "Token状态", example = "ACTIVE", allowableValues = {"ACTIVE", "EXPIRED", "REVOKED", "INVALID"})
    private String status;

    @Schema(description = "用户ID", example = "123456")
    private Long userId;

    @Schema(description = "用户名", example = "john_doe")
    private String username;

    @Schema(description = "会话ID", example = "session_abc123")
    private String sessionId;

    @Schema(description = "客户端ID", example = "web")
    private String clientId;

    @Schema(description = "Token类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "颁发时间")
    private LocalDateTime issuedAt;

    @Schema(description = "过期时间")
    private LocalDateTime expiresAt;

    @Schema(description = "剩余有效秒数", example = "3456")
    private Long expiresIn;

    @Schema(description = "用户角色列表")
    private List<String> roles;

    @Schema(description = "用户权限列表")
    private List<String> permissions;

    @Schema(description = "额外信息：设备信息")
    private String deviceInfo;

    @Schema(description = "额外信息：IP地址")
    private String ipAddress;

    @Schema(description = "错误信息（当valid=false时）")
    private String errorMessage;
}

