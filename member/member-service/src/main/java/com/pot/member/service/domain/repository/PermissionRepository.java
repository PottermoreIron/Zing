package com.pot.member.service.domain.repository;

import com.pot.member.service.domain.model.permission.PermissionAggregate;
import com.pot.member.service.domain.model.permission.PermissionId;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PermissionRepository {

        PermissionAggregate save(PermissionAggregate permission);

        Optional<PermissionAggregate> findById(PermissionId permissionId);

        Optional<PermissionAggregate> findByCode(String permissionCode);

        List<PermissionAggregate> findByRoleId(Long roleId);

        List<PermissionAggregate> findByIds(Set<Long> permissionIds);

        List<PermissionAggregate> findAll();

        void delete(PermissionId permissionId);

        boolean existsByCode(String permissionCode);
}
