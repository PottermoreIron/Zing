package com.pot.member.service.entity;

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

/**
 * 用户第三方平台连接实体
 * <p>
 * 用于管理用户与第三方平台的OAuth连接信息，支持多平台社交登录和数据同步
 * </p>
 *
 * @author Pot
 * @since 2025-09-01 23:25:59
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("member_social_connection")
public class SocialConnection implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 创建时间
     */
    @Null(groups = {Create.class, Update.class}, message = "创建时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_created_at")
    private Long gmtCreatedAt;

    /**
     * 更新时间
     */
    @Null(groups = {Create.class, Update.class}, message = "更新时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_updated_at")
    private Long gmtUpdatedAt;

    /**
     * 软删除时间
     */
    @Null(groups = {Create.class, Update.class}, message = "删除时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_deleted_at")
    private Long gmtDeletedAt;

    /**
     * 用户ID
     */
    @NotNull(groups = {Create.class, Update.class}, message = "用户ID不能为空")
    @Positive(groups = {Create.class, Update.class}, message = "用户ID必须为正整数")
    @TableField("member_id")
    private Long memberId;

    /**
     * 第三方平台提供商，如：wechat、github、google
     */
    @NotBlank(groups = {Create.class, Update.class}, message = "第三方平台提供商不能为空")
    @Length(min = 2, max = 50, groups = {Create.class, Update.class}, message = "平台提供商名称长度必须在2-50个字符之间")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$",
            groups = {Create.class, Update.class},
            message = "平台提供商名称必须为小写字母开头，只能包含小写字母、数字和下划线")
    @TableField("provider")
    private String provider;

    /**
     * 第三方平台用户ID
     */
    @NotBlank(groups = Create.class, message = "第三方平台用户ID不能为空")
    @Length(min = 1, max = 200, groups = {Create.class, Update.class}, message = "第三方平台用户ID长度必须在1-200个字符之间")
    @TableField("provider_member_id")
    private String providerMemberId;

    /**
     * 第三方平台用户名
     */
    @Length(max = 100, groups = {Create.class, Update.class}, message = "第三方平台用户名不能超过100个字符")
    @TableField("provider_username")
    private String providerUsername;

    /**
     * 第三方平台邮箱
     */
    @Email(groups = {Create.class, Update.class}, message = "邮箱格式不正确")
    @Length(max = 200, groups = {Create.class, Update.class}, message = "邮箱地址不能超过200个字符")
    @TableField("provider_email")
    private String providerEmail;

    /**
     * 访问令牌
     */
    @NotBlank(groups = Create.class, message = "访问令牌不能为空")
    @Length(max = 2000, groups = {Create.class, Update.class}, message = "访问令牌不能超过2000个字符")
    @TableField("access_token")
    private String accessToken;

    /**
     * 刷新令牌
     */
    @Length(max = 2000, groups = {Create.class, Update.class}, message = "刷新令牌不能超过2000个字符")
    @TableField("refresh_token")
    private String refreshToken;

    /**
     * 令牌过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_token_expires_at")
    private Long gmtTokenExpiresAt;

    /**
     * 授权范围
     */
    @Length(max = 500, groups = {Create.class, Update.class}, message = "授权范围不能超过500个字符")
    @TableField("scope")
    private String scope;

    /**
     * 连接是否活跃 (0-非活跃, 1-活跃)
     */
    @NotNull(groups = Create.class, message = "连接状态不能为空")
    @Min(value = 0, groups = {Create.class, Update.class}, message = "连接状态值不能小于0")
    @Max(value = 1, groups = {Create.class, Update.class}, message = "连接状态值不能大于1")
    @TableField("is_active")
    private Integer isActive;

    /**
     * 最后同步时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_last_sync_at")
    private LocalDateTime gmtLastSyncAt;

    /**
     * 第三方平台原始数据（JSON格式）
     */
    @Length(max = 5000, groups = {Create.class, Update.class}, message = "第三方平台原始数据不能超过5000个字符")
    @TableField("extend_json")
    private String extendJson;

    /**
     * 连接状态枚举
     */
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

    /**
     * 支持的第三方平台枚举
     */
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

    /**
     * 业务方法 - 获取状态枚举
     */
    public Status getStatusEnum() {
        return this.isActive != null ? Status.fromCode(this.isActive) : null;
    }

    /**
     * 业务方法 - 设置状态
     */
    public void setStatus(Status status) {
        this.isActive = status != null ? status.getCode() : null;
    }

    /**
     * 业务方法 - 获取平台枚举
     */
    public Provider getProviderEnum() {
        return this.provider != null ? Provider.fromCode(this.provider) : null;
    }

    /**
     * 业务方法 - 设置平台
     */
    public void setProvider(Provider provider) {
        this.provider = provider != null ? provider.getCode() : null;
    }

    /**
     * 业务方法 - 判断连接是否活跃
     */
    public boolean isActive() {
        return Status.ACTIVE.getCode().equals(this.isActive);
    }

    /**
     * 业务方法 - 判断令牌是否过期
     */
    public boolean isTokenExpired() {
        return true;
    }

    /**
     * 业务方法 - 判断连接是否有效
     */
    public boolean isValidConnection() {
        return isActive() && !isTokenExpired() &&
                this.accessToken != null && !this.accessToken.trim().isEmpty();
    }

    /**
     * 业务方法 - 激活连接
     */
    public void activate() {
        this.isActive = Status.ACTIVE.getCode();
    }

    /**
     * 业务方法 - 停用连接
     */
    public void deactivate() {
        this.isActive = Status.INACTIVE.getCode();
    }

    /**
     * 业务方法 - 更新令牌信息
     */
    public void updateTokens(String accessToken, String refreshToken, Long expiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.gmtTokenExpiresAt = expiresAt;
    }

    /**
     * 业务方法 - 更新同步时间
     */
    public void updateSyncTime() {
        this.gmtLastSyncAt = LocalDateTime.now();
    }

    /**
     * 业务方法 - 判断是否为同一平台连接
     */
    public boolean isSamePlatformConnection(SocialConnection other) {
        if (other == null) {
            return false;
        }
        return this.memberId != null && this.provider != null &&
                this.memberId.equals(other.getMemberId()) &&
                this.provider.equals(other.getProvider());
    }
}