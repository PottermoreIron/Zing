package com.pot.member.service.controller;

import com.pot.zing.framework.common.model.R;
import com.pot.member.service.domain.service.PermissionDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 角色权限管理控制器
 * <p>
 * 通过领域服务统一处理角色-权限关联的增删操作，并自动发布权限变更事件。
 * </p>
 *
 * @author Pot
 * @since 2025-09-01 23:25:59
 */
@Slf4j
@RestController
@RequestMapping("/api/role-permissions")
@RequiredArgsConstructor
public class RolePermissionController {

    private final PermissionDomainService permissionDomainService;

    /**
     * 为角色添加权限
     */
    @PostMapping
    public R<Void> addPermission(
            @RequestParam Long roleId,
            @RequestParam Long permissionId,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        permissionDomainService.addPermissionToRole(roleId, permissionId, operator);
        log.info("为角色添加权限成功: roleId={}, permissionId={}, operator={}", roleId, permissionId, operator);
        return R.success();
    }

    /**
     * 从角色移除权限
     */
    @DeleteMapping
    public R<Void> removePermission(
            @RequestParam Long roleId,
            @RequestParam Long permissionId,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        permissionDomainService.removePermissionFromRole(roleId, permissionId, operator);
        log.info("从角色移除权限成功: roleId={}, permissionId={}, operator={}", roleId, permissionId, operator);
        return R.success();
    }
}
