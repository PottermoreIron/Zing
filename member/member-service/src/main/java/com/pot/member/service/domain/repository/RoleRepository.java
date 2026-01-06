package com.pot.member.service.domain.repository;

import com.pot.member.service.domain.model.role.RoleAggregate;
import com.pot.member.service.domain.model.role.RoleId;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 角色仓储接口
 * 
 * @author Pot
 * @since 2026-01-06
 */
public interface RoleRepository {

    /**
     * 保存角色
     */
    RoleAggregate save(RoleAggregate role);

    /**
     * 根据ID查找角色
     */
    Optional<RoleAggregate> findById(RoleId roleId);

    /**
     * 根据角色代码查找
     */
    Optional<RoleAggregate> findByCode(String roleCode);

    /**
     * 根据会员ID查找所有角色
     */
    List<RoleAggregate> findByMemberId(Long memberId);

    /**
     * 批量查找角色
     */
    List<RoleAggregate> findByIds(Set<Long> roleIds);

    /**
     * 查找所有角色
     */
    List<RoleAggregate> findAll();

    /**
     * 删除角色
     */
    void delete(RoleId roleId);

    /**
     * 检查角色代码是否存在
     */
    boolean existsByCode(String roleCode);
}
