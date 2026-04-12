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
@TableName("member_permission")
public class Permission implements Serializable {

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

    @TableField("permission_code")
    private String permissionCode;

    @TableField("permission_name")
    private String permissionName;

    @TableField("permission_description")
    private String permissionDescription;

    @TableField("resource_type")
    private String resourceType;

    @TableField("action_type")
    private String actionType;

    @TableField("parent_id")
    private Long parentId;

    @TableField("permission_level")
    private Integer permissionLevel;

    @TableField("is_system_permission")
    private Integer isSystemPermission;

    @TableField("is_active")
    private Integer isActive;

    @TableField("extend_json")
    private String extendJson;

    public static String generatePermissionCode(String resourceType, ActionType actionType) {
        return resourceType.toLowerCase() + "." + actionType.getCode().toLowerCase();
    }

    public ActionType getActionTypeEnum() {
        return this.actionType != null ? ActionType.fromCode(this.actionType) : null;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType != null ? actionType.getCode() : null;
    }

    public Status getStatusEnum() {
        return this.isActive != null ? Status.fromCode(this.isActive) : null;
    }

    public void setStatus(Status status) {
        this.isActive = status != null ? status.getCode() : null;
    }

    public SystemFlag getSystemFlagEnum() {
        return this.isSystemPermission != null ? SystemFlag.fromCode(this.isSystemPermission) : null;
    }

    public void setSystemFlag(SystemFlag systemFlag) {
        this.isSystemPermission = systemFlag != null ? systemFlag.getCode() : null;
    }

    public boolean isEnabled() {
        return Status.ENABLED.getCode().equals(this.isActive);
    }

    public boolean isSystemBuiltin() {
        return SystemFlag.SYSTEM_BUILTIN.getCode().equals(this.isSystemPermission);
    }

    public boolean isTopLevel() {
        return this.parentId == null || this.permissionLevel == 1;
    }

    public void enable() {
        this.isActive = Status.ENABLED.getCode();
    }

    public void disable() {
        this.isActive = Status.DISABLED.getCode();
    }

    public String getResourceTypeFromCode() {
        if (this.permissionCode != null && this.permissionCode.contains(".")) {
            return this.permissionCode.split("\\.")[0];
        }
        return this.resourceType;
    }

    public String getActionTypeFromCode() {
        if (this.permissionCode != null && this.permissionCode.contains(".")) {
            String[] parts = this.permissionCode.split("\\.");
            if (parts.length >= 2) {
                return parts[parts.length - 1].toUpperCase();
            }
        }
        return this.actionType;
    }

    @Getter
    public enum ActionType {
        CREATE("CREATE", "Create"),
        READ("READ", "Read"),
        UPDATE("UPDATE", "Update"),
        DELETE("DELETE", "Delete"),
        EXECUTE("EXECUTE", "Execute"),
        MANAGE("MANAGE", "Manage"),
        ALL("ALL", "All permissions");

        private final String code;
        private final String description;

        ActionType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public static ActionType fromCode(String code) {
            for (ActionType type : ActionType.values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown action type: " + code);
        }
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
            throw new IllegalArgumentException("Unknown permission status: " + code);
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
            throw new IllegalArgumentException("Unknown system permission flag: " + code);
        }
    }
}