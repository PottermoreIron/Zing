package com.pot.auth.service.dto.v1.session;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pot.auth.service.dto.v1.request.CreateSessionRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证会话响应
 * <p>
 * 代表一个已认证的用户会话，包含：
 * 1. 会话标识信息
 * 2. 访问令牌和刷新令牌
 * 3. 用户基本信息
 * 4. 会话元数据（设备、时间、权限等）
 * <p>
 * 符合OAuth 2.0标准响应格式，并扩展了用户信息和会话管理功能
 *
 * @author Pot
 * @since 2025-10-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "认证会话响应")
public class AuthSession {

    /**
     * 会话ID
     * 用于会话管理、追踪、多设备管理等
     */
    @Schema(description = "会话ID", example = "session_abc123xyz")
    private String sessionId;

    /**
     * 访问令牌（Access Token）
     * 用于访问受保护的资源
     * 有效期较短（如1小时）
     */
    @Schema(
            description = "访问令牌",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String accessToken;

    /**
     * 刷新令牌（Refresh Token）
     * 用于获取新的访问令牌
     * 有效期较长（如7天）
     */
    @Schema(
            description = "刷新令牌",
            example = "refresh_token_abc123xyz..."
    )
    private String refreshToken;

    /**
     * 令牌类型
     * 符合OAuth 2.0标准，通常为"Bearer"
     */
    @Schema(
            description = "令牌类型",
            example = "Bearer",
            defaultValue = "Bearer"
    )
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * 访问令牌过期时间（秒）
     */
    @Schema(
            description = "访问令牌过期时间（秒）",
            example = "3600"
    )
    private Long expiresIn;

    /**
     * 刷新令牌过期时间（秒）
     */
    @Schema(
            description = "刷新令牌过期时间（秒）",
            example = "604800"
    )
    private Long refreshExpiresIn;

    /**
     * 用户信息
     */
    @Schema(description = "用户信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserInfo userInfo;

    /**
     * 会话创建时间
     */
    @Schema(description = "会话创建时间", example = "2025-10-25 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /**
     * 会话最后活跃时间
     */
    @Schema(description = "最后活跃时间", example = "2025-10-25 12:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastActiveAt;

    /**
     * 设备信息
     */
    @Schema(description = "设备信息")
    private CreateSessionRequest.DeviceInfo deviceInfo;

    /**
     * 授权范围（Scopes）
     * 表示该会话拥有的权限范围
     */
    @Schema(
            description = "授权范围",
            example = "[\"read:user\", \"write:user\", \"read:posts\"]"
    )
    private List<String> scopes;

    /**
     * 是否是新注册用户
     * 用于前端判断是否需要引导流程
     */
    @Schema(description = "是否是新注册用户", example = "false")
    private Boolean isNewUser;

    /**
     * 认证方式
     * 记录用户使用的认证方式，用于审计和统计
     */
    @Schema(
            description = "认证方式",
            example = "password",
            allowableValues = {"password", "sms_code", "email_code", "authorization_code", "wechat_qrcode"}
    )
    private String authMethod;

    /**
     * 用户信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户信息")
    public static class UserInfo {

        @Schema(description = "用户ID", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long userId;

        @Schema(description = "用户名", example = "john_doe")
        private String username;

        @Schema(description = "昵称", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
        private String nickname;

        @Schema(description = "邮箱", example = "john@example.com")
        private String email;

        @Schema(description = "手机号", example = "13800138000")
        private String phone;

        @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
        private String avatarUrl;

        @Schema(
                description = "账户状态",
                example = "ACTIVE",
                allowableValues = {"ACTIVE", "INACTIVE", "SUSPENDED", "PENDING_VERIFICATION"}
        )
        private String status;

        @Schema(description = "角色列表", example = "[\"USER\", \"VIP\"]")
        private List<String> roles;

        @Schema(description = "权限列表", example = "[\"user:read\", \"post:create\"]")
        private List<String> permissions;

        @Schema(description = "性别", example = "MALE", allowableValues = {"MALE", "FEMALE", "UNKNOWN"})
        private String gender;

        @Schema(description = "是否已验证邮箱", example = "true")
        private Boolean emailVerified;

        @Schema(description = "是否已验证手机号", example = "true")
        private Boolean phoneVerified;

        @Schema(description = "注册时间", example = "2025-01-01 12:00:00")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime registeredAt;

        @Schema(description = "最后登录时间", example = "2025-10-25 12:00:00")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime lastLoginAt;
    }
}