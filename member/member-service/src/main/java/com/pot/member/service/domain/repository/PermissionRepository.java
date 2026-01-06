package com.pot.member.service.domain.repository;

import com.pot.member.service.domain.model.permission.PermissionAggregate;
import com.pot.member.service.domain.model.permission.PermissionId;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 权限仓储接口
 * 
 * @author Pot
 * @since 2026-01-06
 */
public interface PermissionRepository {

    /**
     * 保存权限
     */
    PermissionAggregate save(PermissionAggregate permission);

    /**
     * 根据ID查找权限
     */
    Optional<PermissionAggregate> findById(PermissionId permissionId);

    /**
     * 根据权限代码查找
     */
    Optional<PermissionAggregate> findByCode(String permissionCode);

    /**
     * 根据角色ID查找所有权限
     */
    List<PermissionAggregate> findByRoleId(Long roleId);

    /**
     * 批量查找权限
     */
    List<PermissionAggregate> findByIds(Set<Long> permissionIds);

    /**
     * 查找所有权限
     */
    List<PermissionAggregate> findAll();

    /**
     * 删除权限
     */
    void delete(PermissionId permissionId);

    /**
     * 检查权限代码是否存在
     */
    boolean existsByCode(String permissionCode);
}
