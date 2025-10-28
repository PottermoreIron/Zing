package com.pot.auth.service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 账户绑定信息
 *
 * @author Zing
 * @since 2025-10-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "账户绑定信息")
public class AccountBindingInfo {

    @Schema(description = "绑定ID", example = "1")
    private Long bindingId;

    @Schema(description = "用户ID", example = "123456")
    private Long userId;

    @Schema(description = "OAuth提供商", example = "wechat")
    private String provider;

    @Schema(description = "第三方平台的用户ID", example = "o6_bmjrPTlm6_2sg...")
    private String openId;

    @Schema(description = "第三方平台的联合ID（微信使用）", example = "oUpF8uMuAJO_M2pxb...")
    private String unionId;

    @Schema(description = "第三方平台的昵称", example = "张三")
    private String nickname;

    @Schema(description = "第三方平台的头像", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Schema(description = "第三方平台的邮箱", example = "user@example.com")
    private String email;

    @Schema(description = "是否为主账号", example = "false")
    private Boolean isPrimary;

    @Schema(description = "绑定状态", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "EXPIRED"})
    private String status;

    @Schema(description = "绑定时间")
    private Long boundAt;

    @Schema(description = "最后更新时间")
    private Long updatedAt;

    @Schema(description = "最后使用时间")
    private Long lastUsedAt;

    @Schema(description = "额外信息（JSON格式）")
    private String extraInfo;
}

