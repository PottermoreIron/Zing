package com.pot.member.service.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pot.zing.framework.common.model.R;
import com.pot.member.service.domain.event.PermissionChangeEventPublisher;
import com.pot.member.service.entity.MemberRole;
import com.pot.member.service.service.MemberRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 用户角色关联表 前端控制器
 * </p>
 *
 * @author Pot
 * @since 2025-09-01 23:25:59
 */
@Slf4j
@RestController
@RequestMapping("/memberRole")
@RequiredArgsConstructor
public class MemberRoleController {

    private final MemberRoleService memberRoleService;
    private final PermissionChangeEventPublisher eventPublisher;

    /**
     * 分配角色给会员
     */
    @PostMapping("/assign")
    public R<Void> assignRole(@RequestParam Long memberId,
            @RequestParam Long roleId,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        // 检查是否已存在
        LambdaQueryWrapper<MemberRole> query = new LambdaQueryWrapper<>();
        query.eq(MemberRole::getMemberId, memberId)
                .eq(MemberRole::getRoleId, roleId);

        if (memberRoleService.count(query) > 0) {
            return R.fail("角色已分配");
        }

        // 创建关联
        MemberRole memberRole = new MemberRole();
        memberRole.setMemberId(memberId);
        memberRole.setRoleId(roleId);
        memberRoleService.save(memberRole);

        // 发布权限变更事件
        eventPublisher.publishMemberRoleAssigned(memberId, roleId, operator);

        log.info("分配角色成功: memberId={}, roleId={}, operator={}", memberId, roleId, operator);
        return R.success();
    }

    /**
     * 撤销会员的角色
     */
    @DeleteMapping("/revoke")
    public R<Void> revokeRole(@RequestParam Long memberId,
            @RequestParam Long roleId,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        // 删除关联
        LambdaQueryWrapper<MemberRole> query = new LambdaQueryWrapper<>();
        query.eq(MemberRole::getMemberId, memberId)
                .eq(MemberRole::getRoleId, roleId);

        boolean removed = memberRoleService.remove(query);
        if (!removed) {
            return R.fail("角色关联不存在");
        }

        // 发布权限变更事件
        eventPublisher.publishMemberRoleRevoked(memberId, roleId, operator);

        log.info("撤销角色成功: memberId={}, roleId={}, operator={}", memberId, roleId, operator);
        return R.success();
    }
}
