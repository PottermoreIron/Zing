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
@TableName("member_permission")
public class Permission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    
    @Null(groups = Create.class, message = "新建权限时ID必须为空")
    @NotNull(groups = Update.class, message = "更新权限时ID不能为空")
    @Positive(groups = Update.class, message = "权限ID必须为正整数")
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

    
    @NotBlank(groups = Create.class, message = "权限编码不能为空")
    @Length(min = 3, max = 100, groups = {Create.class, Update.class}, message = "权限编码长度必须在3-100个字符之间")
    @Pattern(regexp = "^[a-z][a-z0-9]*(\\.([a-z][a-z0-9]*))*$",
            groups = {Create.class, Update.class},
            message = "权限编码格式不正确，应为小写字母开头的点分隔格式，如：user.create")
    @TableField("permission_code")
    private String permissionCode;

    
    @NotBlank(groups = Create.class, message = "权限名称不能为空")
    @Length(min = 2, max = 100, groups = {Create.class, Update.class}, message = "权限名称长度必须在2-100个字符之间")
    @TableField("permission_name")
    private String permissionName;

    
    @Length(max = 500, groups = {Create.class, Update.class}, message = "权限描述不能超过500个字符")
    @TableField("permission_description")
    private String permissionDescription;

    
    @NotBlank(groups = Create.class, message = "资源类型不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$",
            groups = {Create.class, Update.class},
            message = "资源类型必须为小写字母开头，只能包含小写字母、数字和下划线")
    @Length(max = 50, groups = {Create.class, Update.class}, message = "资源类型不能超过50个字符")
    @TableField("resource_type")
    private String resourceType;

    
    @NotBlank(groups = Create.class, message = "操作类型不能为空")
    @Pattern(regexp = "^(CREATE|READ|UPDATE|DELETE|EXECUTE|MANAGE|ALL)$",
            groups = {Create.class, Update.class},
            message = "操作类型必须为: CREATE, READ, UPDATE, DELETE, EXECUTE, MANAGE, ALL 中的一种")
    @TableField("action_type")
    private String actionType;

    
    @Positive(groups = {Create.class, Update.class}, message = "父权限ID必须为正整数")
    @TableField("parent_id")
    private Long parentId;

    
    @NotNull(groups = Create.class, message = "权限层级不能为空")
    @Min(value = 1, groups = {Create.class, Update.class}, message = "权限层级最小为1")
    @Max(value = 10, groups = {Create.class, Update.class}, message = "权限层级最大为10")
    @TableField("permission_level")
    private Integer permissionLevel;

    
    @NotNull(groups = Create.class, message = "系统权限标识不能为空")
    @Min(value = 0, groups = {Create.class, Update.class}, message = "系统权限标识值不能小于0")
    @Max(value = 1, groups = {Create.class, Update.class}, message = "系统权限标识值不能大于1")
    @TableField("is_system_permission")
    private Integer isSystemPermission;

    
    @NotNull(groups = Create.class, message = "启用状态不能为空")
    @Min(value = 0, groups = {Create.class, Update.class}, message = "启用状态值不能小于0")
    @Max(value = 1, groups = {Create.class, Update.class}, message = "启用状态值不能大于1")
    @TableField("is_active")
    private Integer isActive;

    
    @Length(max = 2000, groups = {Create.class, Update.class}, message = "扩展配置不能超过2000个字符")
    @TableField("extend_json")
    private String extendJson;

    
    public static String generatePermissionCode(String resourceType, ActionType actionType) {
        return resourceType.toLowerCase() + "." + actionType.getCode().toLowerCase();
    }

    
    public ActionType getActionTypeEnum() {
        return this.actionType != null ? ActionType.fromCode(this.actionType) : null;
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
        CREATE("CREATE", "创建"),
        READ("READ", "读取"),
        UPDATE("UPDATE", "更新"),
        DELETE("DELETE", "删除"),
        EXECUTE("EXECUTE", "执行"),
        MANAGE("MANAGE", "管理"),
        ALL("ALL", "全部权限");

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
            throw new IllegalArgumentException("未知的操作类型: " + code);
        }
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
            throw new IllegalArgumentException("未知的权限状态: " + code);
        }
    }

    
    @Getter
    public enum SystemFlag {
        USER_DEFINED(0, "用户自定义"),
        SYSTEM_BUILTIN(1, "系统内置");

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
            throw new IllegalArgumentException("未知的系统权限标识: " + code);
        }
    }
}