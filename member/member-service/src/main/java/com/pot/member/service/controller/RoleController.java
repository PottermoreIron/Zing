package com.pot.member.service.controller;

import com.pot.member.service.application.command.AssignRoleCommand;
import com.pot.member.service.application.dto.RoleDTO;
import com.pot.member.service.application.service.RoleApplicationService;
import com.pot.zing.framework.common.model.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 角色表 前端控制器
 * </p>
 *
 * @author Pot
 * @since 2025-09-01 23:25:59
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleApplicationService roleApplicationService;

    /**
     * 获取角色信息
     */
    @GetMapping("/{roleId}")
    public R<RoleDTO> getRole(@PathVariable Long roleId) {
        RoleDTO role = roleApplicationService.getRole(roleId);
        return R.success(role);
    }

    /**
     * 根据角色代码获取角色
     */
    @GetMapping("/by-code/{roleCode}")
    public R<RoleDTO> getRoleByCode(@PathVariable String roleCode) {
        RoleDTO role = roleApplicationService.getRoleByCode(roleCode);
        return R.success(role);
    }

    /**
     * 获取所有角色
     */
    @GetMapping
    public R<List<RoleDTO>> getAllRoles() {
        List<RoleDTO> roles = roleApplicationService.getAllRoles();
        return R.success(roles);
    }

    /**
     * 获取会员的所有角色
     */
    @GetMapping("/member/{memberId}")
    public R<List<RoleDTO>> getMemberRoles(@PathVariable Long memberId) {
        List<RoleDTO> roles = roleApplicationService.getMemberRoles(memberId);
        return R.success(roles);
    }

    /**
     * 为会员分配角色
     */
    @PostMapping("/assign")
    public R<Void> assignRole(@RequestBody AssignRoleCommand command) {
        roleApplicationService.assignRole(command);
        return R.success();
    }

    /**
     * 撤销会员的角色
     */
    @DeleteMapping("/member/{memberId}/role/{roleId}")
    public R<Void> revokeRole(
            @PathVariable Long memberId,
            @PathVariable Long roleId,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        roleApplicationService.revokeRole(memberId, roleId, operator);
        return R.success();
    }
}
