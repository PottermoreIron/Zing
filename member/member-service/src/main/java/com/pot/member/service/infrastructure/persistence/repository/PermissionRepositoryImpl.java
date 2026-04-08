package com.pot.member.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pot.member.service.domain.model.permission.PermissionAggregate;
import com.pot.member.service.domain.model.permission.PermissionId;
import com.pot.member.service.domain.repository.PermissionRepository;
import com.pot.member.service.infrastructure.persistence.entity.Permission;
import com.pot.member.service.infrastructure.persistence.entity.RolePermission;
import com.pot.member.service.infrastructure.persistence.mapper.PermissionMapper;
import com.pot.member.service.infrastructure.persistence.mapper.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PermissionRepositoryImpl implements PermissionRepository {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public PermissionAggregate save(PermissionAggregate aggregate) {
        Permission entity = toEntity(aggregate);

        if (entity.getId() == null) {
            permissionMapper.insert(entity);
            log.debug("新增权限: {}", entity.getId());
        } else {
            permissionMapper.updateById(entity);
            log.debug("更新权限: {}", entity.getId());
        }

        return toAggregate(entity);
    }

    @Override
    public Optional<PermissionAggregate> findById(PermissionId permissionId) {
        Permission entity = permissionMapper.selectById(permissionId.value());
        return Optional.ofNullable(entity).map(this::toAggregate);
    }

    @Override
    public Optional<PermissionAggregate> findByCode(String permissionCode) {
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Permission::getPermissionCode, permissionCode);
        Permission entity = permissionMapper.selectOne(wrapper);
        return Optional.ofNullable(entity).map(this::toAggregate);
    }

    @Override
    public boolean existsByCode(String permissionCode) {
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Permission::getPermissionCode, permissionCode);
        return permissionMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<PermissionAggregate> findByRoleId(Long roleId) {
        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getRoleId, roleId);
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(wrapper);

        if (rolePermissions.isEmpty()) {
            return List.of();
        }

        Set<Long> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toSet());

        return findByIds(permissionIds);
    }

    @Override
    public List<PermissionAggregate> findByIds(Set<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Permission::getId, permissionIds);
        List<Permission> permissions = permissionMapper.selectList(wrapper);
        return permissions.stream()
                .map(this::toAggregate)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionAggregate> findAll() {
        List<Permission> permissions = permissionMapper.selectList(null);
        return permissions.stream()
                .map(this::toAggregate)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(PermissionId permissionId) {
        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getPermissionId, permissionId.value());
        rolePermissionMapper.delete(wrapper);

        permissionMapper.deleteById(permissionId.value());
        log.debug("删除权限: {}", permissionId.value());
    }

    private PermissionAggregate toAggregate(Permission entity) {
        return PermissionAggregate.reconstitute(
                PermissionId.of(entity.getId()),
                entity.getPermissionCode(),
                entity.getPermissionName(),
                entity.getResourceType(),
                entity.getActionType(),
                entity.getPermissionDescription(),
                entity.getGmtCreatedAt(),
                entity.getGmtUpdatedAt());
    }

    private Permission toEntity(PermissionAggregate aggregate) {
        Permission entity = new Permission();
        if (aggregate.getPermissionId() != null) {
            entity.setId(aggregate.getPermissionId().value());
        }
        entity.setPermissionCode(aggregate.getPermissionCode());
        entity.setPermissionName(aggregate.getPermissionName());
        entity.setResourceType(aggregate.getResource());
        entity.setActionType(aggregate.getAction());
        entity.setPermissionDescription(aggregate.getDescription());
        entity.setGmtCreatedAt(aggregate.getCreatedAt());
        entity.setGmtUpdatedAt(aggregate.getUpdatedAt());
        return entity;
    }
}
