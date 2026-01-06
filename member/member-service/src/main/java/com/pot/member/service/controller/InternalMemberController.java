package com.pot.member.service.controller;

import com.pot.member.service.application.command.RegisterMemberCommand;
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
 * 会员内部API控制器
 * 供其他服务内部调用
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Slf4j
@RestController
@RequestMapping("/internal/members")
@RequiredArgsConstructor
public class InternalMemberController {

    private final MemberApplicationService memberApplicationService;

    /**
     * 注册新会员（内部API）
     */
    @PostMapping("/register")
    public R<MemberDTO> register(@RequestBody RegisterMemberCommand command) {
        log.info("内部API - 注册新会员: email={}", command.getEmail());
        MemberDTO member = memberApplicationService.register(command);
        return R.success(member);
    }

    /**
     * 根据邮箱获取会员信息
     */
    @GetMapping("/by-email/{email}")
    public R<MemberDTO> getMemberByEmail(@PathVariable String email) {
        GetMemberQuery query = GetMemberQuery.builder()
                .email(email)
                .build();
        MemberDTO member = memberApplicationService.getMember(query);
        return R.success(member);
    }

    /**
     * 根据手机号获取会员信息
     */
    @GetMapping("/by-phone/{phoneNumber}")
    public R<MemberDTO> getMemberByPhoneNumber(@PathVariable String phoneNumber) {
        GetMemberQuery query = GetMemberQuery.builder()
                .phoneNumber(phoneNumber)
                .build();
        MemberDTO member = memberApplicationService.getMember(query);
        return R.success(member);
    }

    /**
     * 根据用户名获取会员信息
     */
    @GetMapping("/by-username/{username}")
    public R<MemberDTO> getMemberByUsername(@PathVariable String username) {
        GetMemberQuery query = GetMemberQuery.builder()
                .username(username)
                .build();
        MemberDTO member = memberApplicationService.getMember(query);
        return R.success(member);
    }

    /**
     * 根据会员ID获取会员信息
     */
    @GetMapping("/{memberId}")
    public R<MemberDTO> getMemberById(@PathVariable Long memberId) {
        GetMemberQuery query = GetMemberQuery.builder()
                .memberId(memberId)
                .build();
        MemberDTO member = memberApplicationService.getMember(query);
        return R.success(member);
    }

    /**
     * 获取会员的所有权限（内部API - 供auth-service调用）
     */
    @GetMapping("/{memberId}/permissions")
    public R<Set<PermissionDTO>> getMemberPermissions(@PathVariable Long memberId) {
        log.debug("内部API - 获取会员权限: memberId={}", memberId);
        GetMemberPermissionsQuery query = GetMemberPermissionsQuery.builder()
                .memberId(memberId)
                .build();
        Set<PermissionDTO> permissions = memberApplicationService.getMemberPermissions(query);
        return R.success(permissions);
    }
}
