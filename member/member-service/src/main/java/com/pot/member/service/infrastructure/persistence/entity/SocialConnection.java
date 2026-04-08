package com.pot.member.service.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("member_social_connection")
public class SocialConnection implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("gmt_created_at")
    private Long gmtCreatedAt;

    @TableField("gmt_updated_at")
    private Long gmtUpdatedAt;

    @TableField("gmt_deleted_at")
    private Long gmtDeletedAt;

    @TableField("member_id")
    private Long memberId;

    @TableField("provider")
    private String provider;

    @TableField("provider_member_id")
    private String providerMemberId;

    @TableField("provider_username")
    private String providerUsername;

    @TableField("provider_email")
    private String providerEmail;

    @TableField("access_token")
    private String accessToken;

    @TableField("refresh_token")
    private String refreshToken;

    @TableField("gmt_token_expires_at")
    private Long gmtTokenExpiresAt;

    @TableField("scope")
    private String scope;

    @TableField("is_active")
    private Integer isActive;

    @TableField("gmt_last_sync_at")
    private LocalDateTime gmtLastSyncAt;

    @TableField("extend_json")
    private String extendJson;

    public Status getStatusEnum() {
        return this.isActive != null ? Status.fromCode(this.isActive) : null;
    }

    public void setStatus(Status status) {
        this.isActive = status != null ? status.getCode() : null;
    }

    public Provider getProviderEnum() {
        return this.provider != null ? Provider.fromCode(this.provider) : null;
    }

    public void setProvider(Provider provider) {
        this.provider = provider != null ? provider.getCode() : null;
    }

    public boolean isActive() {
        return Status.ACTIVE.getCode().equals(this.isActive);
    }

    public boolean isTokenExpired() {
        return true;
    }

    public boolean isValidConnection() {
        return isActive() && !isTokenExpired() &&
                this.accessToken != null && !this.accessToken.trim().isEmpty();
    }

    public void activate() {
        this.isActive = Status.ACTIVE.getCode();
    }

    public void deactivate() {
        this.isActive = Status.INACTIVE.getCode();
    }

    public void updateTokens(String accessToken, String refreshToken, Long expiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.gmtTokenExpiresAt = expiresAt;
    }

    public void updateSyncTime() {
        this.gmtLastSyncAt = LocalDateTime.now();
    }

    public boolean isSamePlatformConnection(SocialConnection other) {
        if (other == null) {
            return false;
        }
        return this.memberId != null && this.provider != null &&
                this.memberId.equals(other.getMemberId()) &&
                this.provider.equals(other.getProvider());
    }

    @Getter
    public enum Status {
        INACTIVE(0, "非活跃"),
        ACTIVE(1, "活跃");

        private final Integer code;
        private final String description;

        Status(Integer code, String description) {
            this.code = code;
            this.description = description;
        }

        public static Status fromCode(Integer code) {
            for (Status status : Status.values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("未知的连接状态: " + code);
        }
    }

    @Getter
    public enum Provider {
        WECHAT("wechat", "微信"),
        QQ("qq", "QQ"),
        WEIBO("weibo", "微博"),
        GITHUB("github", "GitHub"),
        GOOGLE("google", "Google"),
        FACEBOOK("facebook", "Facebook"),
        TWITTER("twitter", "Twitter"),
        LINKEDIN("linkedin", "LinkedIn");

        private final String code;
        private final String description;

        Provider(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public static Provider fromCode(String code) {
            for (Provider provider : Provider.values()) {
                if (provider.code.equals(code)) {
                    return provider;
                }
            }
            throw new IllegalArgumentException("不支持的第三方平台: " + code);
        }
    }
}