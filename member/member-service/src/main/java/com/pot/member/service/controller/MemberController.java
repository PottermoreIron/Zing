package com.pot.member.service.controller;

import com.pot.member.facade.dto.MemberDTO;
import com.pot.member.service.converter.MemberConverter;
import com.pot.member.service.entity.Member;
import com.pot.member.service.service.MemberService;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.security.annotation.PreventResubmit;
import com.pot.zing.framework.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 会员控制器
 * <p>
 * 演示如何使用Spring Security的权限控制
 * </p>
 *
 * @author Pot
 * @since 2025-01-24
 */
@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberConverter memberConverter;

    /**
     * 获取当前登录用户信息
     * 任何认证用户都可以访问
     */
    @GetMapping("/me")
    public R<MemberDTO> getCurrentMember() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("未登录");
        }

        log.info("获取当前用户信息: userId={}", userId);
        Member member = memberService.getById(userId);

        if (member == null) {
            throw new BusinessException("用户不存在");
        }

        return R.success(memberConverter.toDTO(member));
    }

    /**
     * 更新当前用户信息
     * 需要user:update权限
     */
    @PutMapping("/me")
    @PreAuthorize("hasAuthority('user:update')")
    @PreventResubmit(interval = 5, message = "更新操作过于频繁")
    public R<MemberDTO> updateCurrentMember(@RequestBody MemberDTO memberDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("未登录");
        }

        log.info("更新当前用户信息: userId={}", userId);

        Member member = memberService.getById(userId);
        if (member == null) {
            throw new BusinessException("用户不存在");
        }

        // 更新允许修改的字段
        if (memberDTO.getNickname() != null) {
            member.setNickname(memberDTO.getNickname());
        }
        if (memberDTO.getAvatarUrl() != null) {
            member.setAvatarUrl(memberDTO.getAvatarUrl());
        }
        if (memberDTO.getGender() != null) {
            member.setGender(Member.Gender.fromCode(member.getGender()));
        }
        // Birthday field removed from MemberDTO, skip it

        memberService.updateById(member);

        return R.success(memberConverter.toDTO(member), "更新成功");
    }

    /**
     * 根据ID查询用户
     * 需要ADMIN角色或user:read权限
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('user:read')")
    public R<MemberDTO> getMemberById(@PathVariable Long id) {
        log.info("查询用户: userId={}", id);

        Member member = memberService.getById(id);
        if (member == null) {
            throw new BusinessException("用户不存在");
        }

        return R.success(memberConverter.toDTO(member));
    }

    /**
     * 删除用户
     * 需要ADMIN角色和user:delete权限
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('user:delete')")
    public R<Void> deleteMember(@PathVariable Long id) {
        log.info("删除用户: userId={}", id);

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (id.equals(currentUserId)) {
            throw new BusinessException("不能删除自己");
        }

        Member member = memberService.getById(id);
        if (member == null) {
            throw new BusinessException("用户不存在");
        }

        // 软删除
        memberService.updateById(member);
        member.setStatus(Member.AccountStatus.DELETED.getCode()); // 已删除
        return R.success(null, "删除成功");
    }

    /**
     * 获取用户角色
     * 演示如何获取当前用户的角色和权限
     */
    @GetMapping("/me/roles")
    public R<Object> getCurrentUserRoles() {
        Long userId = SecurityUtils.getCurrentUserId();
        String username = SecurityUtils.getCurrentUsername();

        return R.success(
                new Object() {
                    public final Long userId = SecurityUtils.getCurrentUserId();
                    public final String username = SecurityUtils.getCurrentUsername();
                    public final Object roles = SecurityUtils.getCurrentUserRoles();
                    public final Object permissions = SecurityUtils.getCurrentUserPermissions();
                },
                "获取成功"
        );
    }
}

