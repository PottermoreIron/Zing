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
@TableName("member_role")
public class Role implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    
    @Null(groups = Create.class, message = "新建角色时ID必须为空")
    @NotNull(groups = Update.class, message = "更新角色时ID不能为空")
    @Positive(groups = Update.class, message = "角色ID必须为正整数")
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

    
    @NotBlank(groups = Create.class, message = "角色编码不能为空")
    @Length(min = 2, max = 50, groups = {Create.class, Update.class}, message = "角色编码长度必须在2-50个字符之间")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$",
            groups = {Create.class, Update.class},
            message = "角色编码必须为小写字母开头，只能包含小写字母、数字和下划线")
    @TableField("role_code")
    private String roleCode;

    
    @NotBlank(groups = Create.class, message = "角色名称不能为空")
    @Length(min = 2, max = 100, groups = {Create.class, Update.class}, message = "角色名称长度必须在2-100个字符之间")
    @TableField("role_name")
    private String roleName;

    
    @Length(max = 500, groups = {Create.class, Update.class}, message = "角色描述不能超过500个字符")
    @TableField("role_description")
    private String roleDescription;

    
    @NotNull(groups = Create.class, message = "角色级别不能为空")
    @Min(value = 1, groups = {Create.class, Update.class}, message = "角色级别最小为1")
    @Max(value = 100, groups = {Create.class, Update.class}, message = "角色级别最大为100")
    @TableField("role_level")
    private Integer roleLevel;

    
    @NotNull(groups = Create.class, message = "系统角色标识不能为空")
    @Min(value = 0, groups = {Create.class, Update.class}, message = "系统角色标识值不能小于0")
    @Max(value = 1, groups = {Create.class, Update.class}, message = "系统角色标识值不能大于1")
    @TableField("is_system_role")
    private Integer isSystemRole;

    
    @NotNull(groups = Create.class, message = "启用状态不能为空")
    @Min(value = 0, groups = {Create.class, Update.class}, message = "启用状态值不能小于0")
    @Max(value = 1, groups = {Create.class, Update.class}, message = "启用状态值不能大于1")
    @TableField("is_active")
    private Integer isActive;

    
    @Length(max = 2000, groups = {Create.class, Update.class}, message = "扩展配置不能超过2000个字符")
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
}