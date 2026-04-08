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
@TableName("member_member_role")
public class MemberRole implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("gmt_created_at")
    private LocalDateTime gmtCreatedAt;

    @TableField("gmt_updated_at")
    private LocalDateTime gmtUpdatedAt;

    @TableField("gmt_deleted_at")
    private LocalDateTime gmtDeletedAt;

    @TableField("member_id")
    private Long memberId;

    @TableField("role_id")
    private Long roleId;

    @TableField("gmt_expires_at")
    private LocalDateTime gmtExpiresAt;

    @TableField("is_active")
    private Integer isActive;

    @TableField("assignment_note")
    private String assignmentNote;

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

        LocalDateTime baseTime = this.gmtExpiresAt != null ? this.gmtExpiresAt : LocalDateTime.now();
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