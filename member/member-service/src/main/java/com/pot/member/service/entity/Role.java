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
 * 角色实体
 * <p>
 * 用于管理系统角色信息，支持角色层级和权限分组
 * </p>
 *
 * @author Pot
 * @since 2025-09-01 23:25:59
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("member_role")
public class Role implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Null(groups = Create.class, message = "新建角色时ID必须为空")
    @NotNull(groups = Update.class, message = "更新角色时ID不能为空")
    @Positive(groups = Update.class, message = "角色ID必须为正整数")
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
     * 角色编码，如：admin、editor、viewer
     */
    @NotBlank(groups = Create.class, message = "角色编码不能为空")
    @Length(min = 2, max = 50, groups = {Create.class, Update.class}, message = "角色编码长度必须在2-50个字符之间")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$",
            groups = {Create.class, Update.class},
            message = "角色编码必须为小写字母开头，只能包含小写字母、数字和下划线")
    @TableField("role_code")
    private String roleCode;

    /**
     * 角色名称
     */
    @NotBlank(groups = Create.class, message = "角色名称不能为空")
    @Length(min = 2, max = 100, groups = {Create.class, Update.class}, message = "角色名称长度必须在2-100个字符之间")
    @TableField("role_name")
    private String roleName;

    /**
     * 角色描述
     */
    @Length(max = 500, groups = {Create.class, Update.class}, message = "角色描述不能超过500个字符")
    @TableField("role_description")
    private String roleDescription;

    /**
     * 角色级别，数字越大权限越高 (1-100)
     */
    @NotNull(groups = Create.class, message = "角色级别不能为空")
    @Min(value = 1, groups = {Create.class, Update.class}, message = "角色级别最小为1")
    @Max(value = 100, groups = {Create.class, Update.class}, message = "角色级别最大为100")
    @TableField("role_level")
    private Integer roleLevel;

    /**
     * 是否为系统内置角色 (0-否, 1-是)
     */
    @NotNull(groups = Create.class, message = "系统角色标识不能为空")
    @Min(value = 0, groups = {Create.class, Update.class}, message = "系统角色标识值不能小于0")
    @Max(value = 1, groups = {Create.class, Update.class}, message = "系统角色标识值不能大于1")
    @TableField("is_system_role")
    private Integer isSystemRole;

    /**
     * 是否启用 (0-禁用, 1-启用)
     */
    @NotNull(groups = Create.class, message = "启用状态不能为空")
    @Min(value = 0, groups = {Create.class, Update.class}, message = "启用状态值不能小于0")
    @Max(value = 1, groups = {Create.class, Update.class}, message = "启用状态值不能大于1")
    @TableField("is_active")
    private Integer isActive;

    /**
     * 角色扩展配置（JSON格式）
     */
    @Length(max = 2000, groups = {Create.class, Update.class}, message = "扩展配置不能超过2000个字符")
    @TableField("extend_json")
    private String extendJson;

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

    /**
     * 系统角色标识枚举
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
            throw new IllegalArgumentException("未知的系统角色标识: " + code);
        }
    }

    /**
     * 预定义角色常量
     */
    @Getter
    public enum PredefinedRoles {
        SUPER_ADMIN("super_admin", "超级管理员"),
        ADMIN("admin", "管理员");

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
            throw new IllegalArgumentException("未知的预定义角色: " + code);
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
     * 业务方法 - 获取系统角色标识枚举
     */
    public SystemFlag getSystemFlagEnum() {
        return this.isSystemRole != null ? SystemFlag.fromCode(this.isSystemRole) : null;
    }

    /**
     * 业务方法 - 设置系统角色标识
     */
    public void setSystemFlag(SystemFlag systemFlag) {
        this.isSystemRole = systemFlag != null ? systemFlag.getCode() : null;
    }

    /**
     * 业务方法 - 判断是否启用
     */
    public boolean isEnabled() {
        return Status.ENABLED.getCode().equals(this.isActive);
    }

    /**
     * 业务方法 - 判断是否为系统内置角色
     */
    public boolean isSystemBuiltin() {
        return SystemFlag.SYSTEM_BUILTIN.getCode().equals(this.isSystemRole);
    }

    /**
     * 业务方法 - 判断是否可删除（非系统角色且未启用）
     */
    public boolean isDeletable() {
        return !isSystemBuiltin() && !isEnabled();
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
     * 业务方法 - 判断角色级别是否高于指定角色
     */
    public boolean hasHigherLevelThan(Role otherRole) {
        if (this.roleLevel == null || otherRole.getRoleLevel() == null) {
            return false;
        }
        return this.roleLevel > otherRole.getRoleLevel();
    }

    /**
     * 业务方法 - 判断是否为管理员角色
     */
    public boolean isAdminRole() {
        return PredefinedRoles.ADMIN.getCode().equals(this.roleCode) ||
                PredefinedRoles.SUPER_ADMIN.getCode().equals(this.roleCode);
    }
}