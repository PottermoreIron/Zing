package com.pot.member.service.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.pot.zing.framework.common.validate.Create;
import com.pot.zing.framework.common.validate.Update;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("member_social_connection")
public class SocialConnection implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    
    @Null(groups = {Create.class, Update.class}, message = "创建时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_created_at")
    private Long gmtCreatedAt;

    
    @Null(groups = {Create.class, Update.class}, message = "更新时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_updated_at")
    private Long gmtUpdatedAt;

    
    @Null(groups = {Create.class, Update.class}, message = "删除时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_deleted_at")
    private Long gmtDeletedAt;

    
    @NotNull(groups = {Create.class, Update.class}, message = "用户ID不能为空")
    @Positive(groups = {Create.class, Update.class}, message = "用户ID必须为正整数")
    @TableField("member_id")
    private Long memberId;

    
    @NotBlank(groups = {Create.class, Update.class}, message = "第三方平台提供商不能为空")
    @Length(min = 2, max = 50, groups = {Create.class, Update.class}, message = "平台提供商名称长度必须在2-50个字符之间")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$",
            groups = {Create.class, Update.class},
            message = "平台提供商名称必须为小写字母开头，只能包含小写字母、数字和下划线")
    @TableField("provider")
    private String provider;

    
    @NotBlank(groups = Create.class, message = "第三方平台用户ID不能为空")
    @Length(min = 1, max = 200, groups = {Create.class, Update.class}, message = "第三方平台用户ID长度必须在1-200个字符之间")
    @TableField("provider_member_id")
    private String providerMemberId;

    
    @Length(max = 100, groups = {Create.class, Update.class}, message = "第三方平台用户名不能超过100个字符")
    @TableField("provider_username")
    private String providerUsername;

    
    @Email(groups = {Create.class, Update.class}, message = "邮箱格式不正确")
    @Length(max = 200, groups = {Create.class, Update.class}, message = "邮箱地址不能超过200个字符")
    @TableField("provider_email")
    private String providerEmail;

    
    @NotBlank(groups = Create.class, message = "访问令牌不能为空")
    @Length(max = 2000, groups = {Create.class, Update.class}, message = "访问令牌不能超过2000个字符")
    @TableField("access_token")
    private String accessToken;

    
    @Length(max = 2000, groups = {Create.class, Update.class}, message = "刷新令牌不能超过2000个字符")
    @TableField("refresh_token")
    private String refreshToken;

    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_token_expires_at")
    private Long gmtTokenExpiresAt;

    
    @Length(max = 500, groups = {Create.class, Update.class}, message = "授权范围不能超过500个字符")
    @TableField("scope")
    private String scope;

    
    @NotNull(groups = Create.class, message = "连接状态不能为空")
    @Min(value = 0, groups = {Create.class, Update.class}, message = "连接状态值不能小于0")
    @Max(value = 1, groups = {Create.class, Update.class}, message = "连接状态值不能大于1")
    @TableField("is_active")
    private Integer isActive;

    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_last_sync_at")
    private LocalDateTime gmtLastSyncAt;

    
    @Length(max = 5000, groups = {Create.class, Update.class}, message = "第三方平台原始数据不能超过5000个字符")
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