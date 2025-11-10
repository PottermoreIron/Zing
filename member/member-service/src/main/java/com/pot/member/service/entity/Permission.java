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
 * 权限实体
 * <p>
 * 用于管理系统权限信息，支持层级结构和资源-操作模式
 * </p>
 *
 * @author Pot
 * @since 2025-09-01 23:25:59
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("member_permission")
public class Permission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Null(groups = Create.class, message = "新建权限时ID必须为空")
    @NotNull(groups = Update.class, message = "更新权限时ID不能为空")
    @Positive(groups = Update.class, message = "权限ID必须为正整数")
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
     * 权限编码，如：user.create、content.edit
     */
    @NotBlank(groups = Create.class, message = "权限编码不能为空")
    @Length(min = 3, max = 100, groups = {Create.class, Update.class}, message = "权限编码长度必须在3-100个字符之间")
    @Pattern(regexp = "^[a-z][a-z0-9]*(\\.([a-z][a-z0-9]*))*$",
            groups = {Create.class, Update.class},
            message = "权限编码格式不正确，应为小写字母开头的点分隔格式，如：user.create")
    @TableField("permission_code")
    private String permissionCode;

    /**
     * 权限名称
     */
    @NotBlank(groups = Create.class, message = "权限名称不能为空")
    @Length(min = 2, max = 100, groups = {Create.class, Update.class}, message = "权限名称长度必须在2-100个字符之间")
    @TableField("permission_name")
    private String permissionName;

    /**
     * 权限描述
     */
    @Length(max = 500, groups = {Create.class, Update.class}, message = "权限描述不能超过500个字符")
    @TableField("permission_description")
    private String permissionDescription;

    /**
     * 资源类型，如：user、content、system
     */
    @NotBlank(groups = Create.class, message = "资源类型不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$",
            groups = {Create.class, Update.class},
            message = "资源类型必须为小写字母开头，只能包含小写字母、数字和下划线")
    @Length(max = 50, groups = {Create.class, Update.class}, message = "资源类型不能超过50个字符")
    @TableField("resource_type")
    private String resourceType;

    /**
     * 操作类型，如：create、read、update、delete
     */
    @NotBlank(groups = Create.class, message = "操作类型不能为空")
    @Pattern(regexp = "^(CREATE|READ|UPDATE|DELETE|EXECUTE|MANAGE|ALL)$",
            groups = {Create.class, Update.class},
            message = "操作类型必须为: CREATE, READ, UPDATE, DELETE, EXECUTE, MANAGE, ALL 中的一种")
    @TableField("action_type")
    private String actionType;

    /**
     * 父权限ID，支持权限分组
     */
    @Positive(groups = {Create.class, Update.class}, message = "父权限ID必须为正整数")
    @TableField("parent_id")
    private Long parentId;

    /**
     * 权限层级 (1-顶级, 2-二级, 以此类推)
     */
    @NotNull(groups = Create.class, message = "权限层级不能为空")
    @Min(value = 1, groups = {Create.class, Update.class}, message = "权限层级最小为1")
    @Max(value = 10, groups = {Create.class, Update.class}, message = "权限层级最大为10")
    @TableField("permission_level")
    private Integer permissionLevel;

    /**
     * 是否为系统内置权限 (0-否, 1-是)
     */
    @NotNull(groups = Create.class, message = "系统权限标识不能为空")
    @Min(value = 0, groups = {Create.class, Update.class}, message = "系统权限标识值不能小于0")
    @Max(value = 1, groups = {Create.class, Update.class}, message = "系统权限标识值不能大于1")
    @TableField("is_system_permission")
    private Integer isSystemPermission;

    /**
     * 是否启用 (0-禁用, 1-启用)
     */
    @NotNull(groups = Create.class, message = "启用状态不能为空")
    @Min(value = 0, groups = {Create.class, Update.class}, message = "启用状态值不能小于0")
    @Max(value = 1, groups = {Create.class, Update.class}, message = "启用状态值不能大于1")
    @TableField("is_active")
    private Integer isActive;

    /**
     * 权限扩展配置（JSON格式）
     */
    @Length(max = 2000, groups = {Create.class, Update.class}, message = "扩展配置不能超过2000个字符")
    @TableField("extend_json")
    private String extendJson;

    /**
     * 业务方法 - 生成权限编码
     */
    public static String generatePermissionCode(String resourceType, ActionType actionType) {
        return resourceType.toLowerCase() + "." + actionType.getCode().toLowerCase();
    }

    /**
     * 业务方法 - 获取操作类型枚举
     */
    public ActionType getActionTypeEnum() {
        return this.actionType != null ? ActionType.fromCode(this.actionType) : null;
    }

    /**
     * 业务方法 - 设置操作类型
     */
    public void setActionType(ActionType actionType) {
        this.actionType = actionType != null ? actionType.getCode() : null;
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
     * 业务方法 - 获取系统权限标识枚举
     */
    public SystemFlag getSystemFlagEnum() {
        return this.isSystemPermission != null ? SystemFlag.fromCode(this.isSystemPermission) : null;
    }

    /**
     * 业务方法 - 设置系统权限标识
     */
    public void setSystemFlag(SystemFlag systemFlag) {
        this.isSystemPermission = systemFlag != null ? systemFlag.getCode() : null;
    }

    /**
     * 业务方法 - 判断是否启用
     */
    public boolean isEnabled() {
        return Status.ENABLED.getCode().equals(this.isActive);
    }

    /**
     * 业务方法 - 判断是否为系统内置权限
     */
    public boolean isSystemBuiltin() {
        return SystemFlag.SYSTEM_BUILTIN.getCode().equals(this.isSystemPermission);
    }

    /**
     * 业务方法 - 判断是否为顶级权限
     */
    public boolean isTopLevel() {
        return this.parentId == null || this.permissionLevel == 1;
    }

    /**
     * 业务方法 - 启用权限
     */
    public void enable() {
        this.isActive = Status.ENABLED.getCode();
    }

    /**
     * 业务方法 - 禁用权限
     */
    public void disable() {
        this.isActive = Status.DISABLED.getCode();
    }

    /**
     * 业务方法 - 解析权限编码获取资源类型
     */
    public String getResourceTypeFromCode() {
        if (this.permissionCode != null && this.permissionCode.contains(".")) {
            return this.permissionCode.split("\\.")[0];
        }
        return this.resourceType;
    }

    /**
     * 业务方法 - 解析权限编码获取操作类型
     */
    public String getActionTypeFromCode() {
        if (this.permissionCode != null && this.permissionCode.contains(".")) {
            String[] parts = this.permissionCode.split("\\.");
            if (parts.length >= 2) {
                return parts[parts.length - 1].toUpperCase();
            }
        }
        return this.actionType;
    }

    /**
     * 操作类型枚举
     */
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

    /**
     * 状态枚举
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
            throw new IllegalArgumentException("未知的权限状态: " + code);
        }
    }

    /**
     * 系统权限标识枚举
     */
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