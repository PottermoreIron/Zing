package com.pot.member.service.domain.service;

import com.pot.member.service.domain.event.PermissionChangedEvent;
import com.pot.member.service.domain.model.member.MemberAggregate;
import com.pot.member.service.domain.model.permission.PermissionAggregate;
import com.pot.member.service.domain.model.role.RoleAggregate;
import com.pot.member.service.domain.port.DomainEventPublisher;
import com.pot.member.service.domain.repository.MemberRepository;
import com.pot.member.service.domain.repository.PermissionRepository;
import com.pot.member.service.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Domain service for permission and role assignment flows.
 *
 * @author Pot
 * @since 2026-01-06
 */
@Slf4j
@RequiredArgsConstructor
public class PermissionDomainService {

        private final MemberRepository memberRepository;
        private final RoleRepository roleRepository;
        private final PermissionRepository permissionRepository;
        private final DomainEventPublisher eventPublisher;

        /**
         * Assign a role to a member.
         */
        public void assignRoleToMember(Long memberId, Long roleId, String operator) {
                MemberAggregate member = memberRepository
                                .findById(com.pot.member.service.domain.model.member.MemberId.of(memberId))
                                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

                roleRepository.findById(com.pot.member.service.domain.model.role.RoleId.of(roleId))
                                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

                member.assignRole(roleId);
                memberRepository.save(member);

                eventPublisher.publish(new PermissionChangedEvent(
                                String.valueOf(memberId),
                                Set.of(memberId),
                                PermissionChangedEvent.ChangeType.MEMBER_ROLE_ASSIGNED,
                                roleId, null, operator));

                log.info("[Permission] Assigning role — memberId={}, roleId={}", memberId, roleId);
        }

        /**
         * Revoke a role from a member.
         */
        public void revokeRoleFromMember(Long memberId, Long roleId, String operator) {
                MemberAggregate member = memberRepository
                                .findById(com.pot.member.service.domain.model.member.MemberId.of(memberId))
                                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

                member.revokeRole(roleId);
                memberRepository.save(member);

                eventPublisher.publish(new PermissionChangedEvent(
                                String.valueOf(memberId),
                                Set.of(memberId),
                                PermissionChangedEvent.ChangeType.MEMBER_ROLE_REVOKED,
                                roleId, null, operator));

                log.info("[Permission] Revoking role — memberId={}, roleId={}", memberId, roleId);
        }

        /**
         * Add a permission to a role.
         */
        public void addPermissionToRole(Long roleId, Long permissionId, String operator) {
                RoleAggregate role = roleRepository.findById(com.pot.member.service.domain.model.role.RoleId.of(roleId))
                                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

                permissionRepository
                                .findById(com.pot.member.service.domain.model.permission.PermissionId.of(permissionId))
                                .orElseThrow(() -> new IllegalArgumentException("Permission not found"));

                role.addPermission(permissionId);
                roleRepository.save(role);

                Set<Long> affectedMemberIds = findMemberIdsByRoleId(roleId);

                if (!affectedMemberIds.isEmpty()) {
                        eventPublisher.publish(new PermissionChangedEvent(
                                        String.valueOf(roleId),
                                        affectedMemberIds,
                                        PermissionChangedEvent.ChangeType.ROLE_PERMISSION_ADDED,
                                        roleId, permissionId, operator));
                }

                log.info("[Permission] Adding permission to role — roleId={}, permissionId={}, affectedMembers={}", roleId, permissionId, affectedMemberIds.size());
        }

        /**
         * Remove a permission from a role.
         */
        public void removePermissionFromRole(Long roleId, Long permissionId, String operator) {
                RoleAggregate role = roleRepository.findById(com.pot.member.service.domain.model.role.RoleId.of(roleId))
                                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

                role.removePermission(permissionId);
                roleRepository.save(role);

                Set<Long> affectedMemberIds = findMemberIdsByRoleId(roleId);

                if (!affectedMemberIds.isEmpty()) {
                        eventPublisher.publish(new PermissionChangedEvent(
                                        String.valueOf(roleId),
                                        affectedMemberIds,
                                        PermissionChangedEvent.ChangeType.ROLE_PERMISSION_REMOVED,
                                        roleId, permissionId, operator));
                }

                log.info("[Permission] Removing permission from role — roleId={}, permissionId={}, affectedMembers={}", roleId, permissionId, affectedMemberIds.size());
        }

        /**
         * Get all permissions granted to a member.
         */
        public Set<PermissionAggregate> getMemberPermissions(Long memberId) {
                MemberAggregate member = memberRepository
                                .findById(com.pot.member.service.domain.model.member.MemberId.of(memberId))
                                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

                Map<Long, Set<Long>> permissionIdsByRole = roleRepository
                                .findPermissionIdsByRoleIds(member.getRoleIds());

                Set<Long> permissionIds = permissionIdsByRole.values().stream()
                                .flatMap(Set::stream)
                                .collect(Collectors.toSet());

                return new HashSet<>(permissionRepository.findByIds(permissionIds));
        }

        /**
         * Batch load permission codes by member ID.
         */
        public Map<Long, Set<String>> getMemberPermissionCodesBatch(List<Long> memberIds) {
                if (memberIds == null || memberIds.isEmpty()) {
                        return Map.of();
                }

                Set<Long> requestedMemberIds = new LinkedHashSet<>(memberIds);
                Map<Long, Set<Long>> roleIdsByMember = memberRepository.findRoleIdsByMemberIds(requestedMemberIds);

                Set<Long> allRoleIds = roleIdsByMember.values().stream()
                                .flatMap(Set::stream)
                                .collect(Collectors.toSet());
                Map<Long, Set<Long>> permissionIdsByRole = roleRepository.findPermissionIdsByRoleIds(allRoleIds);

                Set<Long> allPermissionIds = permissionIdsByRole.values().stream()
                                .flatMap(Set::stream)
                                .collect(Collectors.toSet());
                Map<Long, String> permissionCodeById = permissionRepository.findByIds(allPermissionIds).stream()
                                .collect(Collectors.toMap(
                                                permission -> permission.getPermissionId().value(),
                                                PermissionAggregate::getPermissionCode));

                Map<Long, Set<String>> permissionCodesByMember = new LinkedHashMap<>();
                for (Long memberId : requestedMemberIds) {
                        Set<String> permissionCodes = roleIdsByMember.getOrDefault(memberId, Set.of()).stream()
                                        .flatMap(roleId -> permissionIdsByRole.getOrDefault(roleId, Set.of()).stream())
                                        .map(permissionCodeById::get)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toSet());
                        permissionCodesByMember.put(memberId, permissionCodes);
                }
                return permissionCodesByMember;
        }

        /**
         * Find all member IDs that currently hold the given role.
         */
        private Set<Long> findMemberIdsByRoleId(Long roleId) {
                return memberRepository.findMemberIdsByRoleId(roleId);
        }
}
