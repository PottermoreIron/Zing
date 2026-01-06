package com.pot.member.service.domain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pot.member.service.domain.event.PermissionChangeEventPublisher;
import com.pot.member.service.domain.model.member.MemberAggregate;
import com.pot.member.service.domain.model.permission.PermissionAggregate;
import com.pot.member.service.domain.model.role.RoleAggregate;
import com.pot.member.service.domain.repository.MemberRepository;
import com.pot.member.service.domain.repository.PermissionRepository;
import com.pot.member.service.domain.repository.RoleRepository;
import com.pot.member.service.entity.MemberRole;
import com.pot.member.service.mapper.MemberRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限领域服务
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionDomainService {

    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PermissionChangeEventPublisher eventPublisher;
    private final MemberRoleMapper memberRoleMapper;

    /**
     * 为会员分配角色
     */
    public void assignRoleToMember(Long memberId, Long roleId, String operator) {
        MemberAggregate member = memberRepository
                .findById(com.pot.member.service.domain.model.member.MemberId.of(memberId))
                .orElseThrow(() -> new IllegalArgumentException("会员不存在"));

        RoleAggregate role = roleRepository.findById(com.pot.member.service.domain.model.role.RoleId.of(roleId))
                .orElseThrow(() -> new IllegalArgumentException("角色不存在"));

        member.assignRole(roleId);
        memberRepository.save(member);

        // 发布权限变更事件
        eventPublisher.publishMemberRoleAssigned(memberId, roleId, operator);

        log.info("为会员{}分配角色{}", memberId, roleId);
    }

    /**
     * 撤销会员的角色
     */
    public void revokeRoleFromMember(Long memberId, Long roleId, String operator) {
        MemberAggregate member = memberRepository
                .findById(com.pot.member.service.domain.model.member.MemberId.of(memberId))
                .orElseThrow(() -> new IllegalArgumentException("会员不存在"));

        member.revokeRole(roleId);
        memberRepository.save(member);

        // 发布权限变更事件
        eventPublisher.publishMemberRoleRevoked(memberId, roleId, operator);

        log.info("撤销会员{}的角色{}", memberId, roleId);
    }

    /**
     * 为角色添加权限
     */
    public void addPermissionToRole(Long roleId, Long permissionId, String operator) {
        RoleAggregate role = roleRepository.findById(com.pot.member.service.domain.model.role.RoleId.of(roleId))
                .orElseThrow(() -> new IllegalArgumentException("角色不存在"));

        PermissionAggregate permission = permissionRepository
                .findById(com.pot.member.service.domain.model.permission.PermissionId.of(permissionId))
                .orElseThrow(() -> new IllegalArgumentException("权限不存在"));

        role.addPermission(permissionId);
        roleRepository.save(role);

        // 查找所有拥有该角色的会员
        Set<Long> affectedMemberIds = findMemberIdsByRoleId(roleId);

        // 发布权限变更事件
        if (!affectedMemberIds.isEmpty()) {
            eventPublisher.publishRolePermissionAdded(affectedMemberIds, roleId, permissionId, operator);
        }

        log.info("为角色{}添加权限{}，影响{}个会员", roleId, permissionId, affectedMemberIds.size());
    }

    /**
     * 从角色移除权限
     */
    public void removePermissionFromRole(Long roleId, Long permissionId, String operator) {
        RoleAggregate role = roleRepository.findById(com.pot.member.service.domain.model.role.RoleId.of(roleId))
                .orElseThrow(() -> new IllegalArgumentException("角色不存在"));

        role.removePermission(permissionId);
        roleRepository.save(role);

        // 查找所有拥有该角色的会员
        Set<Long> affectedMemberIds = findMemberIdsByRoleId(roleId);

        // 发布权限变更事件
        if (!affectedMemberIds.isEmpty()) {
            eventPublisher.publishRolePermissionRemoved(affectedMemberIds, roleId, permissionId, operator);
        }

        log.info("从角色{}移除权限{}，影响{}个会员", roleId, permissionId, affectedMemberIds.size());
    }

    /**
     * 获取会员的所有权限
     */
    public Set<PermissionAggregate> getMemberPermissions(Long memberId) {
        MemberAggregate member = memberRepository
                .findById(com.pot.member.service.domain.model.member.MemberId.of(memberId))
                .orElseThrow(() -> new IllegalArgumentException("会员不存在"));

        // 获取会员的所有角色
        List<RoleAggregate> roles = roleRepository.findByIds(member.getRoleIds());

        // 收集所有权限ID
        Set<Long> permissionIds = roles.stream()
                .flatMap(role -> role.getPermissionIds().stream())
                .collect(Collectors.toSet());

        // 获取所有权限
        return new HashSet<>(permissionRepository.findByIds(permissionIds));
    }

    /**
     * 查找拥有指定角色的所有会员ID
     */
    private Set<Long> findMemberIdsByRoleId(Long roleId) {
        LambdaQueryWrapper<MemberRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MemberRole::getRoleId, roleId);
        List<MemberRole> memberRoles = memberRoleMapper.selectList(wrapper);
        return memberRoles.stream()
                .map(MemberRole::getMemberId)
                .collect(Collectors.toSet());
    }
}
