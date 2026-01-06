package com.pot.member.service.controller;

import com.pot.member.service.application.command.ChangePasswordCommand;
import com.pot.member.service.application.command.UpdateMemberProfileCommand;
import com.pot.member.service.application.dto.MemberDTO;
import com.pot.member.service.application.dto.PermissionDTO;
import com.pot.member.service.application.query.GetMemberPermissionsQuery;
import com.pot.member.service.application.query.GetMemberQuery;
import com.pot.member.service.application.service.MemberApplicationService;
import com.pot.zing.framework.common.model.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

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

    private final MemberApplicationService memberApplicationService;

    /**
     * 获取会员信息
     */
    @GetMapping("/{memberId}")
    public R<MemberDTO> getMember(@PathVariable Long memberId) {
        GetMemberQuery query = GetMemberQuery.builder()
                .memberId(memberId)
                .build();
        MemberDTO member = memberApplicationService.getMember(query);
        return R.success(member);
    }

    /**
     * 获取当前登录会员信息
     */
    @GetMapping("/me")
    public R<MemberDTO> getCurrentMember(@RequestAttribute("memberId") Long memberId) {
        GetMemberQuery query = GetMemberQuery.builder()
                .memberId(memberId)
                .build();
        MemberDTO member = memberApplicationService.getMember(query);
        return R.success(member);
    }

    /**
     * 更新会员资料
     */
    @PutMapping("/{memberId}/profile")
    public R<MemberDTO> updateProfile(
            @PathVariable Long memberId,
            @RequestBody UpdateMemberProfileCommand command) {
        command.setMemberId(memberId);
        MemberDTO member = memberApplicationService.updateProfile(command);
        return R.success(member);
    }

    /**
     * 修改密码
     */
    @PutMapping("/{memberId}/password")
    public R<Void> changePassword(
            @PathVariable Long memberId,
            @RequestBody ChangePasswordCommand command) {
        command.setMemberId(memberId);
        memberApplicationService.changePassword(command);
        return R.success();
    }

    /**
     * 获取会员权限
     */
    @GetMapping("/{memberId}/permissions")
    public R<Set<PermissionDTO>> getMemberPermissions(@PathVariable Long memberId) {
        GetMemberPermissionsQuery query = GetMemberPermissionsQuery.builder()
                .memberId(memberId)
                .build();
        Set<PermissionDTO> permissions = memberApplicationService.getMemberPermissions(query);
        return R.success(permissions);
    }

    /**
     * 锁定会员
     */
    @PostMapping("/{memberId}/lock")
    public R<Void> lockMember(@PathVariable Long memberId) {
        memberApplicationService.lockMember(memberId);
        return R.success();
    }

    /**
     * 解锁会员
     */
    @PostMapping("/{memberId}/unlock")
    public R<Void> unlockMember(@PathVariable Long memberId) {
        memberApplicationService.unlockMember(memberId);
        return R.success();
    }
}
