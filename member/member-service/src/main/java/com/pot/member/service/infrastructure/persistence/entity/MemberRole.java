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
@TableName("member_member_role")
public class MemberRole implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    
    @Null(groups = {Create.class, Update.class}, message = "创建时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_created_at")
    private LocalDateTime gmtCreatedAt;

    
    @Null(groups = {Create.class, Update.class}, message = "更新时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_updated_at")
    private LocalDateTime gmtUpdatedAt;

    
    @Null(groups = {Create.class, Update.class}, message = "删除时间由系统自动生成，不可手动设置")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_deleted_at")
    private LocalDateTime gmtDeletedAt;

    
    @NotNull(groups = {Create.class, Update.class}, message = "用户ID不能为空")
    @Positive(groups = {Create.class, Update.class}, message = "用户ID必须为正整数")
    @TableField("member_id")
    private Long memberId;

    
    @NotNull(groups = {Create.class, Update.class}, message = "角色ID不能为空")
    @Positive(groups = {Create.class, Update.class}, message = "角色ID必须为正整数")
    @TableField("role_id")
    private Long roleId;

    
    @Future(groups = {Create.class, Update.class}, message = "过期时间必须是未来时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("gmt_expires_at")
    private LocalDateTime gmtExpiresAt;

    
    @NotNull(groups = Create.class, message = "新建角色关联时启用状态不能为空")
    @Min(value = 0, groups = {Create.class, Update.class}, message = "启用状态值不能小于0")
    @Max(value = 1, groups = {Create.class, Update.class}, message = "启用状态值不能大于1")
    @TableField("is_active")
    private Integer isActive;

    
    @Length(max = 500, groups = {Create.class, Update.class}, message = "分配备注不能超过500个字符")
    @TableField("assignment_note")
    private String assignmentNote;

    
    @Length(max = 2000, groups = {Create.class, Update.class}, message = "扩展信息不能超过2000个字符")
    @TableField("extend_json")
    private String extendJson;

    
    public boolean isEnabled() {
        return Status.ENABLED.getCode().equals(this.isActive);
    }

    
    public boolean isDisabled() {
        return Status.DISABLED.getCode().equals(this.isActive);
    }

    
    public void enable() {
        this.isActive = Status.ENABLED.getCode();
    }

    
    public void disable() {
        this.isActive = Status.DISABLED.getCode();
    }

    
    public Status getStatusEnum() {
        return this.isActive != null ? Status.fromCode(this.isActive) : null;
    }

    
    public void setStatus(Status status) {
        this.isActive = status != null ? status.getCode() : null;
    }

    
    public boolean isExpired() {
        return this.gmtExpiresAt != null && this.gmtExpiresAt.isBefore(LocalDateTime.now());
    }

    
    public boolean isPermanent() {
        return this.gmtExpiresAt == null;
    }

    
    public boolean isValid() {
        return isEnabled() && !isExpired();
    }

    
    public void setPermanent() {
        this.gmtExpiresAt = null;
    }

    
    public void setExpiresInDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("有效天数必须大于0");
        }
        this.gmtExpiresAt = LocalDateTime.now().plusDays(days);
    }

    
    public void extendExpiration(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("延长天数必须大于0");
        }

        LocalDateTime baseTime = this.gmtExpiresAt != null ?
                this.gmtExpiresAt : LocalDateTime.now();
        this.gmtExpiresAt = baseTime.plusDays(days);
    }

    
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