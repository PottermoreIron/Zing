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
 * 用户角色关联实体
 * <p>
 * 用于管理用户与角色的关联关系，支持角色过期时间、启用状态等高级功能
 * </p>
 *
 * @author Pot
 * @since 2025-09-01 23:25:59
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("member_member_role")
public class MemberRole implements Serializable {

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
    private LocalDateTime gmtCreatedAt;

    /**
     * 更新时间
     */
    @Null(groups = {Create.class, Update.class}, message = "更新时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_updated_at")
    private LocalDateTime gmtUpdatedAt;

    /**
     * 软删除时间
     */
    @Null(groups = {Create.class, Update.class}, message = "删除时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_deleted_at")
    private LocalDateTime gmtDeletedAt;

    /**
     * 用户ID
     */
    @NotNull(groups = {Create.class, Update.class}, message = "用户ID不能为空")
    @Positive(groups = {Create.class, Update.class}, message = "用户ID必须为正整数")
    @TableField("member_id")
    private Long memberId;

    /**
     * 角色ID
     */
    @NotNull(groups = {Create.class, Update.class}, message = "角色ID不能为空")
    @Positive(groups = {Create.class, Update.class}, message = "角色ID必须为正整数")
    @TableField("role_id")
    private Long roleId;

    /**
     * 过期时间，NULL表示永久有效
     */
    @Future(groups = {Create.class, Update.class}, message = "过期时间必须是未来时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_expires_at")
    private LocalDateTime gmtExpiresAt;

    /**
     * 是否启用 (0-禁用, 1-启用)
     */
    @NotNull(groups = Create.class, message = "新建角色关联时启用状态不能为空")
    @Min(value = 0, groups = {Create.class, Update.class}, message = "启用状态值不能小于0")
    @Max(value = 1, groups = {Create.class, Update.class}, message = "启用状态值不能大于1")
    @TableField("is_active")
    private Integer isActive;

    /**
     * 分配备注
     */
    @Length(max = 500, groups = {Create.class, Update.class}, message = "分配备注不能超过500个字符")
    @TableField("assignment_note")
    private String assignmentNote;

    /**
     * 分配扩展信息（JSON格式）
     */
    @Length(max = 2000, groups = {Create.class, Update.class}, message = "扩展信息不能超过2000个字符")
    @TableField("extend_json")
    private String extendJson;

    /**
     * 业务方法 - 判断角色是否启用
     *
     * @return true-启用，false-禁用
     */
    public boolean isEnabled() {
        return Status.ENABLED.getCode().equals(this.isActive);
    }

    /**
     * 业务方法 - 判断角色是否禁用
     *
     * @return true-禁用，false-启用
     */
    public boolean isDisabled() {
        return Status.DISABLED.getCode().equals(this.isActive);
    }

    /**
     * 业务方法 - 启用角色
     */
    public void enable() {
        this.isActive = Status.ENABLED.getCode();
    }

    /**
     * 业务方法 - 禁用角色
     */
    public void disable() {
        this.isActive = Status.DISABLED.getCode();
    }

    /**
     * 业务方法 - 获取状态枚举
     *
     * @return 状态枚举
     */
    public Status getStatusEnum() {
        return this.isActive != null ? Status.fromCode(this.isActive) : null;
    }

    /**
     * 业务方法 - 设置状态
     *
     * @param status 状态枚举
     */
    public void setStatus(Status status) {
        this.isActive = status != null ? status.getCode() : null;
    }

    /**
     * 业务方法 - 判断角色是否已过期
     *
     * @return true-已过期，false-未过期或永久有效
     */
    public boolean isExpired() {
        return this.gmtExpiresAt != null && this.gmtExpiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * 业务方法 - 判断角色是否永久有效
     *
     * @return true-永久有效，false-有过期时间
     */
    public boolean isPermanent() {
        return this.gmtExpiresAt == null;
    }

    /**
     * 业务方法 - 判断角色是否有效（未过期且启用）
     *
     * @return true-有效，false-无效
     */
    public boolean isValid() {
        return isEnabled() && !isExpired();
    }

    /**
     * 业务方法 - 设置永久有效
     */
    public void setPermanent() {
        this.gmtExpiresAt = null;
    }

    /**
     * 业务方法 - 设置过期时间（天数）
     *
     * @param days 有效天数
     */
    public void setExpiresInDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("有效天数必须大于0");
        }
        this.gmtExpiresAt = LocalDateTime.now().plusDays(days);
    }

    /**
     * 业务方法 - 延长有效期
     *
     * @param days 延长天数
     */
    public void extendExpiration(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("延长天数必须大于0");
        }

        LocalDateTime baseTime = this.gmtExpiresAt != null ?
                this.gmtExpiresAt : LocalDateTime.now();
        this.gmtExpiresAt = baseTime.plusDays(days);
    }

    /**
     * 角色状态枚举
     */
    @Getter
    public enum Status {
        DISABLED(0, "禁用"),
        ENABLED(1, "启用");

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
            throw new IllegalArgumentException("未知的角色状态: " + code);
        }
    }
}