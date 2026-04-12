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
@TableName("member_role")
public class Role implements Serializable {

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

    @TableField("role_code")
    private String roleCode;

    @TableField("role_name")
    private String roleName;

    @TableField("role_description")
    private String roleDescription;

    @TableField("role_level")
    private Integer roleLevel;

    @TableField("is_system_role")
    private Integer isSystemRole;

    @TableField("is_active")
    private Integer isActive;

    @TableField("extend_json")
    private String extendJson;

    public Status getStatusEnum() {
        return this.isActive != null ? Status.fromCode(this.isActive) : null;
    }

    public void setStatus(Status status) {
        this.isActive = status != null ? status.getCode() : null;
    }

    public SystemFlag getSystemFlagEnum() {
        return this.isSystemRole != null ? SystemFlag.fromCode(this.isSystemRole) : null;
    }

    public void setSystemFlag(SystemFlag systemFlag) {
        this.isSystemRole = systemFlag != null ? systemFlag.getCode() : null;
    }

    public boolean isEnabled() {
        return Status.ENABLED.getCode().equals(this.isActive);
    }

    public boolean isSystemBuiltin() {
        return SystemFlag.SYSTEM_BUILTIN.getCode().equals(this.isSystemRole);
    }

    public boolean isDeletable() {
        return !isSystemBuiltin() && !isEnabled();
    }

    public void enable() {
        this.isActive = Status.ENABLED.getCode();
    }

    public void disable() {
        this.isActive = Status.DISABLED.getCode();
    }

    public boolean hasHigherLevelThan(Role otherRole) {
        if (this.roleLevel == null || otherRole.getRoleLevel() == null) {
            return false;
        }
        return this.roleLevel > otherRole.getRoleLevel();
    }

    public boolean isAdminRole() {
        return PredefinedRoles.ADMIN.getCode().equals(this.roleCode) ||
                PredefinedRoles.SUPER_ADMIN.getCode().equals(this.roleCode);
    }

    @Getter
    public enum Status {
        DISABLED(0, "Disabled"),
        ENABLED(1, "Enabled");

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
            throw new IllegalArgumentException("Unknown role status: " + code);
        }
    }

    @Getter
    public enum SystemFlag {
        USER_DEFINED(0, "User-defined"),
        SYSTEM_BUILTIN(1, "System built-in");

        private final Integer code;
        private final String description;

        SystemFlag(Integer code, String description) {
            this.code = code;
            this.description = description;
        }

        public static SystemFlag fromCode(Integer code) {
            for (SystemFlag flag : SystemFlag.values()) {
                if (flag.code.equals(code)) {
                    return flag;
                }
            }
            throw new IllegalArgumentException("Unknown system role flag: " + code);
        }
    }

    @Getter
    public enum PredefinedRoles {
        SUPER_ADMIN("super_admin", "Super Admin"),
        ADMIN("admin", "Administrator");

        private final String code;
        private final String description;

        PredefinedRoles(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public static PredefinedRoles fromCode(String code) {
            for (PredefinedRoles role : PredefinedRoles.values()) {
                if (role.code.equals(code)) {
                    return role;
                }
            }
            throw new IllegalArgumentException("Unknown predefined role: " + code);
        }
    }
}