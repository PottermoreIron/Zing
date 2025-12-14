package com.pot.member.service.controller.internal;

import com.pot.member.service.service.PermissionQueryInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 内部权限查询接口
 *
 * <p>
 * 提供给auth-service的内部接口，用于查询用户权限：
 * <ul>
 * <li>批量查询用户权限</li>
 * <li>查询用户角色</li>
 * <li>用于权限缓存的数据源</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@RestController
@RequestMapping("/internal/member")
@RequiredArgsConstructor
public class InternalPermissionController {

    private final PermissionQueryInternalService permissionService;

    /**
     * 查询用户权限
     *
     * @param userId 用户ID
     * @return 用户权限集合
     */
    @GetMapping("/{userId}/permissions")
    public Set<String> queryUserPermissions(@PathVariable String userId) {
        log.info("[内部接口] 查询用户权限: userId={}", userId);
        return permissionService.queryUserPermissions(userId);
    }

    /**
     * 查询用户角色
     *
     * @param userId 用户ID
     * @return 用户角色集合
     */
    @GetMapping("/{userId}/roles")
    public Set<String> queryUserRoles(@PathVariable String userId) {
        log.info("[内部接口] 查询用户角色: userId={}", userId);
        return permissionService.queryUserRoles(userId);
    }

    /**
     * 批量查询用户权限
     *
     * @param userIds 用户ID列表
     * @return 用户ID -> 权限集合的映射
     */
    @PostMapping("/permissions/batch")
    public java.util.Map<String, Set<String>> batchQueryUserPermissions(
            @RequestBody Set<String> userIds) {
        log.info("[内部接口] 批量查询用户权限: count={}", userIds.size());
        return permissionService.batchQueryUserPermissions(userIds);
    }
}
