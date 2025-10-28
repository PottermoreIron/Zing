package com.pot.member.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.pot.zing.framework.common.validate.Create;
import com.pot.zing.framework.common.validate.Update;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色权限关联实体
 * <p>
 * 用于管理角色与权限的多对多关联关系，支持权限的动态分配和回收
 * </p>
 *
 * @author Pot
 * @since 2025-09-01 23:25:59
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("member_role_permission")
public class RolePermission implements Serializable {

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
     * 角色ID
     */
    @NotNull(groups = {Create.class, Update.class}, message = "角色ID不能为空")
    @Positive(groups = {Create.class, Update.class}, message = "角色ID必须为正整数")
    @TableField("role_id")
    private Long roleId;

    /**
     * 权限ID
     */
    @NotNull(groups = {Create.class, Update.class}, message = "权限ID不能为空")
    @Positive(groups = {Create.class, Update.class}, message = "权限ID必须为正整数")
    @TableField("permission_id")
    private Long permissionId;

    /**
     * 业务方法 - 判断关联关系是否有效
     */
    public boolean isValid() {
        return this.roleId != null && this.permissionId != null &&
                this.roleId > 0 && this.permissionId > 0;
    }

    /**
     * 业务方法 - 判断是否为同一关联关系
     */
    public boolean isSameRelation(RolePermission other) {
        if (other == null) {
            return false;
        }
        return this.roleId != null && this.permissionId != null &&
                this.roleId.equals(other.getRoleId()) &&
                this.permissionId.equals(other.getPermissionId());
    }
}