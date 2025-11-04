package com.pot.member.facade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 社交账号连接信息DTO
 * <p>
 * 用于跨服务传输社交账号连接信息，提供给Auth服务等外部服务使用
 * </p>
 *
 * @author Zing
 * @since 2025-11-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialConnectionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 连接ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long memberId;

    /**
     * 第三方平台提供商
     * 如：wechat、github、google等
     */
    private String provider;

    /**
     * 第三方平台用户ID
     */
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
     * 头像URL（从扩展信息中提取）
     */
    private String avatarUrl;

    /**
     * 是否活跃
     * true-活跃，false-非活跃
     */
    private Boolean isActive;

    /**
     * 绑定时间（Unix时间戳）
     */
    private Long boundAt;

    /**
     * 更新时间（Unix时间戳）
     */
    private Long updatedAt;

    /**
     * 最后使用时间（Unix时间戳）
     */
    private Long lastUsedAt;

    /**
     * 是否为主账号
     */
    private Boolean isPrimary;

    /**
     * 连接状态描述
     * 如：ACTIVE、INACTIVE
     */
    private String status;
}