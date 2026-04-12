package com.pot.member.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pot.member.service.domain.model.role.RoleAggregate;
import com.pot.member.service.domain.model.role.RoleId;
import com.pot.member.service.domain.model.role.RoleName;
import com.pot.member.service.domain.repository.RoleRepository;
import com.pot.member.service.infrastructure.persistence.entity.Role;
import com.pot.member.service.infrastructure.persistence.entity.RolePermission;
import com.pot.member.service.infrastructure.persistence.mapper.RoleMapper;
import com.pot.member.service.infrastructure.persistence.mapper.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final com.pot.member.service.infrastructure.persistence.mapper.MemberRoleMapper memberRoleMapper;

    @Override
    public RoleAggregate save(RoleAggregate aggregate) {
        Role entity = toEntity(aggregate);

        if (entity.getId() == null) {
            roleMapper.insert(entity);
            log.debug("[Role] Inserting role — id={}", entity.getId());
        } else {
            roleMapper.updateById(entity);
            log.debug("[Role] Updating role — id={}", entity.getId());
        }

        updateRolePermissions(entity.getId(), aggregate.getPermissionIds());

        return toAggregate(entity, aggregate.getPermissionIds());
    }

    @Override
    public Optional<RoleAggregate> findById(RoleId roleId) {
        Role entity = roleMapper.selectById(roleId.value());
        if (entity == null) {
            return Optional.empty();
        }
        Set<Long> permissionIds = findPermissionIdsByRoleIds(Set.of(roleId.value()))
                .getOrDefault(roleId.value(), Set.of());
        return Optional.of(toAggregate(entity, permissionIds));
    }

    @Override
    public Optional<RoleAggregate> findByCode(String roleCode) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getRoleCode, roleCode);
        Role entity = roleMapper.selectOne(wrapper);
        if (entity == null) {
            return Optional.empty();
        }
        Set<Long> permissionIds = findPermissionIdsByRoleIds(Set.of(entity.getId()))
                .getOrDefault(entity.getId(), Set.of());
        return Optional.of(toAggregate(entity, permissionIds));
    }

    @Override
    public boolean existsByCode(String roleCode) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getRoleCode, roleCode);
        return roleMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<RoleAggregate> findByMemberId(Long memberId) {
        LambdaQueryWrapper<com.pot.member.service.infrastructure.persistence.entity.MemberRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.pot.member.service.infrastructure.persistence.entity.MemberRole::getMemberId, memberId);
        List<com.pot.member.service.infrastructure.persistence.entity.MemberRole> memberRoles = memberRoleMapper
                .selectList(wrapper);

        if (memberRoles.isEmpty()) {
            return List.of();
        }

        Set<Long> roleIds = memberRoles.stream()
                .map(com.pot.member.service.infrastructure.persistence.entity.MemberRole::getRoleId)
                .collect(Collectors.toSet());

        return findByIds(roleIds);
    }

    @Override
    public List<RoleAggregate> findByIds(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Role::getId, roleIds);
        List<Role> roles = roleMapper.selectList(wrapper);
        Map<Long, Set<Long>> permissionIdsByRole = findPermissionIdsByRoleIds(roleIds);
        return roles.stream()
                .map(role -> toAggregate(role, permissionIdsByRole.getOrDefault(role.getId(), Set.of())))
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, Set<Long>> findPermissionIdsByRoleIds(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Map.of();
        }

        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(RolePermission::getRoleId, roleIds);
        return rolePermissionMapper.selectList(wrapper).stream()
                .collect(Collectors.groupingBy(
                        RolePermission::getRoleId,
                        Collectors.mapping(RolePermission::getPermissionId, Collectors.toSet())));
    }

    @Override
    public List<RoleAggregate> findAll() {
        List<Role> roles = roleMapper.selectList(null);
        Set<Long> roleIds = roles.stream()
                .map(Role::getId)
                .collect(Collectors.toSet());
        Map<Long, Set<Long>> permissionIdsByRole = findPermissionIdsByRoleIds(roleIds);
        return roles.stream()
                .map(role -> toAggregate(role, permissionIdsByRole.getOrDefault(role.getId(), Set.of())))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(RoleId roleId) {
        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getRoleId, roleId.value());
        rolePermissionMapper.delete(wrapper);

        roleMapper.deleteById(roleId.value());
        log.debug("[Role] Deleting role — id={}", roleId.value());
    }

        private void updateRolePermissions(Long roleId, Set<Long> permissionIds) {
        LambdaQueryWrapper<RolePermission> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(RolePermission::getRoleId, roleId);
        rolePermissionMapper.delete(deleteWrapper);

        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(roleId);
                rolePermission.setPermissionId(permissionId);
                rolePermissionMapper.insert(rolePermission);
            }
        }
    }

        private RoleAggregate toAggregate(Role entity, Set<Long> permissionIds) {
        return RoleAggregate.reconstitute(
                RoleId.of(entity.getId()),
                RoleName.of(entity.getRoleName()),
                entity.getRoleCode(),
                entity.getRoleDescription(),
                permissionIds,
                entity.getGmtCreatedAt(),
                entity.getGmtUpdatedAt());
    }

        private Role toEntity(RoleAggregate aggregate) {
        Role entity = new Role();
        if (aggregate.getRoleId() != null) {
            entity.setId(aggregate.getRoleId().value());
        }
        entity.setRoleName(aggregate.getRoleName().getValue());
        entity.setRoleCode(aggregate.getRoleCode());
        entity.setRoleDescription(aggregate.getDescription());
        entity.setGmtCreatedAt(aggregate.getCreatedAt());
        entity.setGmtUpdatedAt(aggregate.getUpdatedAt());
        return entity;
    }
}
