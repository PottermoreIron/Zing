package com.pot.member.service.domain.repository;

import com.pot.member.service.domain.model.role.RoleAggregate;
import com.pot.member.service.domain.model.role.RoleId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface RoleRepository {

        RoleAggregate save(RoleAggregate role);

        Optional<RoleAggregate> findById(RoleId roleId);

        Optional<RoleAggregate> findByCode(String roleCode);

        List<RoleAggregate> findByMemberId(Long memberId);

        List<RoleAggregate> findByIds(Set<Long> roleIds);

        Map<Long, Set<Long>> findPermissionIdsByRoleIds(Set<Long> roleIds);

        List<RoleAggregate> findAll();

        void delete(RoleId roleId);

        boolean existsByCode(String roleCode);
}
