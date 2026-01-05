package com.pot.member.service.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pot.zing.framework.common.model.R;
import com.pot.member.service.domain.event.PermissionChangeEventPublisher;
import com.pot.member.service.entity.MemberRole;
import com.pot.member.service.entity.RolePermission;
import com.pot.member.service.service.MemberRoleService;
import com.pot.member.service.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色权限关联表 前端控制器
 * </p>
 *
 * @author Pot
 * @since 2025-09-01 23:25:59
 */
@Slf4j
@RestController
@RequestMapping("/rolePermission")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;
    private final MemberRoleService memberRoleService;
    private final PermissionChangeEventPublisher eventPublisher;

    /**
     * 为角色添加权限
     */
    @PostMapping("/add")
    public R<Void> addPermission(@RequestParam Long roleId,
            @RequestParam Long permissionId,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        // 检查是否已存在
        LambdaQueryWrapper<RolePermission> query = new LambdaQueryWrapper<>();
        query.eq(RolePermission::getRoleId, roleId)
                .eq(RolePermission::getPermissionId, permissionId);

        if (rolePermissionService.count(query) > 0) {
            return R.fail("权限已分配给该角色");
        }

        // 创建关联
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRoleId(roleId);
        rolePermission.setPermissionId(permissionId);
        rolePermissionService.save(rolePermission);

        // 查找所有拥有该角色的会员
        Set<Long> affectedMemberIds = getAffectedMemberIds(roleId);

        // 发布权限变更事件
        if (!affectedMemberIds.isEmpty()) {
            eventPublisher.publishRolePermissionAdded(affectedMemberIds, roleId, permissionId, operator);
        }

        log.info("为角色添加权限成功: roleId={}, permissionId={}, affectedMembers={}, operator={}",
                roleId, permissionId, affectedMemberIds.size(), operator);
        return R.success();
    }

    /**
     * 从角色移除权限
     */
    @DeleteMapping("/remove")
    public R<Void> removePermission(@RequestParam Long roleId,
            @RequestParam Long permissionId,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        // 删除关联
        LambdaQueryWrapper<RolePermission> query = new LambdaQueryWrapper<>();
        query.eq(RolePermission::getRoleId, roleId)
                .eq(RolePermission::getPermissionId, permissionId);

        boolean removed = rolePermissionService.remove(query);
        if (!removed) {
            return R.fail("权限关联不存在");
        }

        // 查找所有拥有该角色的会员
        Set<Long> affectedMemberIds = getAffectedMemberIds(roleId);

        // 发布权限变更事件
        if (!affectedMemberIds.isEmpty()) {
            eventPublisher.publishRolePermissionRemoved(affectedMemberIds, roleId, permissionId, operator);
        }

        log.info("从角色移除权限成功: roleId={}, permissionId={}, affectedMembers={}, operator={}",
                roleId, permissionId, affectedMemberIds.size(), operator);
        return R.success();
    }

    /**
     * 获取拥有指定角色的所有会员ID
     */
    private Set<Long> getAffectedMemberIds(Long roleId) {
        LambdaQueryWrapper<MemberRole> query = new LambdaQueryWrapper<>();
        query.eq(MemberRole::getRoleId, roleId);

        return memberRoleService.list(query).stream()
                .map(MemberRole::getMemberId)
                .collect(Collectors.toSet());
    }
}
