package com.pot.member.service.interfaces.rest;

import com.pot.member.service.application.command.ChangePasswordCommand;
import com.pot.member.service.application.command.UpdateMemberProfileCommand;
import com.pot.member.service.application.dto.MemberDTO;
import com.pot.member.service.application.dto.PermissionDTO;
import com.pot.member.service.application.query.GetMemberPermissionsQuery;
import com.pot.member.service.application.query.GetMemberQuery;
import com.pot.member.service.application.service.MemberAccountApplicationService;
import com.pot.member.service.application.service.MemberApplicationService;
import com.pot.member.service.application.service.MemberPermissionApplicationService;
import com.pot.member.service.application.service.MemberQueryApplicationService;
import com.pot.member.service.interfaces.rest.request.ChangePasswordRequest;
import com.pot.member.service.interfaces.rest.request.UpdateMemberProfileRequest;
import com.pot.zing.framework.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * REST controller for member operations.
 *
 * @author Pot
 * @since 2025-01-24
 */
@Slf4j
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Validated
@Tag(name = "会员", description = "会员资料、密码与权限管理")
public class MemberController {

    private final MemberAccountApplicationService memberAccountApplicationService;
    private final MemberApplicationService memberApplicationService;
    private final MemberPermissionApplicationService memberPermissionApplicationService;
    private final MemberQueryApplicationService memberQueryApplicationService;

    /**
     * Get member details.
     */
    @Operation(summary = "查询会员详情")
    @GetMapping("/{memberId}")
    public R<MemberDTO> getMember(@PathVariable Long memberId) {
        GetMemberQuery query = GetMemberQuery.byMemberId(memberId);
        MemberDTO member = memberQueryApplicationService.getMember(query);
        return R.success(member);
    }

    /**
     * Get the current member.
     */
    @Operation(summary = "查询当前会员")
    @GetMapping("/me")
    public R<MemberDTO> getCurrentMember(@RequestAttribute("memberId") Long memberId) {
        GetMemberQuery query = GetMemberQuery.byMemberId(memberId);
        MemberDTO member = memberQueryApplicationService.getMember(query);
        return R.success(member);
    }

    /**
     * Update the member profile.
     */
    @Operation(summary = "更新会员资料")
    @PutMapping("/{memberId}/profile")
    public R<MemberDTO> updateProfile(
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberProfileRequest request) {
        UpdateMemberProfileCommand command = UpdateMemberProfileCommand.builder()
                .memberId(memberId)
                .nickname(request.nickname())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .gender(request.gender())
                .birthDate(request.birthDate())
                .bio(request.bio())
                .countryCode(request.countryCode())
                .region(request.region())
                .city(request.city())
                .timezone(request.timezone())
                .locale(request.locale())
                .build();
        MemberDTO member = memberApplicationService.updateProfile(command);
        return R.success(member);
    }

    /**
     * Change the member password.
     */
    @Operation(summary = "修改会员密码")
    @PutMapping("/{memberId}/password")
    public R<Void> changePassword(
            @PathVariable Long memberId,
            @Valid @RequestBody ChangePasswordRequest request) {
        ChangePasswordCommand command = new ChangePasswordCommand(
                memberId,
                request.oldPassword(),
                request.newPassword());
        memberAccountApplicationService.changePassword(command);
        return R.success();
    }

    /**
     * Get member permissions.
     */
    @Operation(summary = "查询会员权限")
    @GetMapping("/{memberId}/permissions")
    public R<Set<PermissionDTO>> getMemberPermissions(@PathVariable Long memberId) {
        GetMemberPermissionsQuery query = GetMemberPermissionsQuery.ofMemberId(memberId);
        Set<PermissionDTO> permissions = memberPermissionApplicationService.getMemberPermissions(query);
        return R.success(permissions);
    }

    /**
     * Lock a member.
     */
    @Operation(summary = "锁定会员")
    @PostMapping("/{memberId}/lock")
    public R<Void> lockMember(@PathVariable Long memberId) {
        memberAccountApplicationService.lockMember(memberId);
        return R.success();
    }

    /**
     * Unlock a member.
     */
    @Operation(summary = "解锁会员")
    @PostMapping("/{memberId}/unlock")
    public R<Void> unlockMember(@PathVariable Long memberId) {
        memberAccountApplicationService.unlockMember(memberId);
        return R.success();
    }
}
