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
@TableName("member_role_permission")
public class RolePermission implements Serializable {

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

    @TableField("role_id")
    private Long roleId;

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