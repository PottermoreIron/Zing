package com.pot.member.service.infrastructure.persistence.entity;

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


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("member_role_permission")
public class RolePermission implements Serializable {

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

    
    @NotNull(groups = {Create.class, Update.class}, message = "角色ID不能为空")
    @Positive(groups = {Create.class, Update.class}, message = "角色ID必须为正整数")
    @TableField("role_id")
    private Long roleId;

    
    @NotNull(groups = {Create.class, Update.class}, message = "权限ID不能为空")
    @Positive(groups = {Create.class, Update.class}, message = "权限ID必须为正整数")
    @TableField("permission_id")
    private Long permissionId;

    
    public boolean isValid() {
        return this.roleId != null && this.permissionId != null &&
                this.roleId > 0 && this.permissionId > 0;
    }

    
    public boolean isSameRelation(RolePermission other) {
        if (other == null) {
            return false;
        }
        return this.roleId != null && this.permissionId != null &&
                this.roleId.equals(other.getRoleId()) &&
                this.permissionId.equals(other.getPermissionId());
    }
}