package com.pot.member.facade.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 绑定社交账号请求DTO
 * <p>
 * 用于Auth服务向Member服务请求绑定社交账号
 * </p>
 *
 * @author Zing
 * @since 2025-11-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindSocialAccountRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long memberId;

    /**
     * 第三方平台提供商
     * 如：wechat、github、google等
     */
    @NotBlank(message = "平台提供商不能为空")
    private String provider;

    /**
     * 第三方平台用户ID
     */
    @NotBlank(message = "第三方平台用户ID不能为空")
    private String providerMemberId;

    /**
     * 第三方平台用户名
     */
    private String providerUsername;

    /**
     * 第三方平台邮箱
     */
    private String providerEmail;

    /**
     * 访问令牌
     */
    @NotBlank(message = "访问令牌不能为空")
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 令牌过期时间（Unix时间戳）
     */
    private Long tokenExpiresAt;

    /**
     * 授权范围
     * 如：snsapi_userinfo、user:email等
     */
    private String scope;

    /**
     * 扩展信息 (JSON格式)
     * 可存储头像URL、昵称等额外信息
     */
    private String extendJson;
}


