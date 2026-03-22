package com.pot.member.service.domain.model.social;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 社交账号连接聚合根
 *
 * <p>
 * 每条记录代表一个会员与一个第三方 OAuth2 平台的绑定关系。
 * 该聚合根是 member 聚合的关联聚合，通过 memberId 引用（跨聚合只引用 ID）。
 *
 * @author Pot
 * @since 2026-03-18
 */
@Getter
public class SocialConnectionAggregate {

    private Long id;
    private Long memberId;
    private String provider;
    private String providerMemberId;
    private String providerUsername;
    private String providerEmail;
    private String accessToken;
    private String refreshToken;
    private Long tokenExpiresAt;
    private String scope;
    private String extendJson;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private SocialConnectionAggregate() {
    }

    /**
     * 创建新社交连接
     */
    public static SocialConnectionAggregate create(Long memberId,
                                                   String provider,
                                                   String providerMemberId,
                                                   String providerUsername,
                                                   String providerEmail,
                                                   String accessToken,
                                                   String refreshToken,
                                                   Long tokenExpiresAt,
                                                   String scope,
                                                   String extendJson) {
        SocialConnectionAggregate conn = new SocialConnectionAggregate();
        conn.memberId = memberId;
        conn.provider = provider.toLowerCase();
        conn.providerMemberId = providerMemberId;
        conn.providerUsername = providerUsername;
        conn.providerEmail = providerEmail;
        conn.accessToken = accessToken;
        conn.refreshToken = refreshToken;
        conn.tokenExpiresAt = tokenExpiresAt;
        conn.scope = scope;
        conn.extendJson = extendJson;
        conn.active = true;
        conn.createdAt = LocalDateTime.now();
        conn.updatedAt = LocalDateTime.now();
        return conn;
    }

    /**
     * 重建（从数据库）
     */
    public static SocialConnectionAggregate reconstitute(Long id, Long memberId, String provider,
                                                         String providerMemberId, String providerUsername,
                                                         String providerEmail, String accessToken,
                                                         String refreshToken, Long tokenExpiresAt,
                                                         String scope, String extendJson,
                                                         boolean active, LocalDateTime createdAt,
                                                         LocalDateTime updatedAt, LocalDateTime deletedAt) {
        SocialConnectionAggregate conn = new SocialConnectionAggregate();
        conn.id = id;
        conn.memberId = memberId;
        conn.provider = provider;
        conn.providerMemberId = providerMemberId;
        conn.providerUsername = providerUsername;
        conn.providerEmail = providerEmail;
        conn.accessToken = accessToken;
        conn.refreshToken = refreshToken;
        conn.tokenExpiresAt = tokenExpiresAt;
        conn.scope = scope;
        conn.extendJson = extendJson;
        conn.active = active;
        conn.createdAt = createdAt;
        conn.updatedAt = updatedAt;
        conn.deletedAt = deletedAt;
        return conn;
    }

    /**
     * 更新 token
     */
    public void updateTokens(String accessToken, String refreshToken, Long tokenExpiresAt) {
        this.accessToken = accessToken;
        if (refreshToken != null && !refreshToken.isBlank()) {
            this.refreshToken = refreshToken;
        }
        if (tokenExpiresAt != null) {
            this.tokenExpiresAt = tokenExpiresAt;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 软删除（解绑）
     */
    public void deactivate() {
        this.active = false;
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
