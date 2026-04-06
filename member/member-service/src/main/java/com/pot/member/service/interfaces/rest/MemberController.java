package com.pot.member.service.interfaces.rest;

import com.pot.member.service.application.command.ChangePasswordCommand;
import com.pot.member.service.application.command.UpdateMemberProfileCommand;
import com.pot.member.service.application.dto.MemberDTO;
import com.pot.member.service.application.dto.PermissionDTO;
import com.pot.member.service.application.query.GetMemberPermissionsQuery;
import com.pot.member.service.application.query.GetMemberQuery;
import com.pot.member.service.application.service.MemberApplicationService;
import com.pot.member.service.application.service.MemberPermissionApplicationService;
import com.pot.zing.framework.common.model.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class MemberController {

    private final MemberApplicationService memberApplicationService;
    private final MemberPermissionApplicationService memberPermissionApplicationService;

    /**
     * Get member details.
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
     * Get the current member.
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
     * Update the member profile.
     */
    @PutMapping("/{memberId}/profile")
    public R<MemberDTO> updateProfile(
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberProfileCommand command) {
        command.setMemberId(memberId);
        MemberDTO member = memberApplicationService.updateProfile(command);
        return R.success(member);
    }

    /**
     * Change the member password.
     */
    @PutMapping("/{memberId}/password")
    public R<Void> changePassword(
            @PathVariable Long memberId,
            @Valid @RequestBody ChangePasswordCommand command) {
        command.setMemberId(memberId);
        memberApplicationService.changePassword(command);
        return R.success();
    }

    /**
     * Get member permissions.
     */
    @GetMapping("/{memberId}/permissions")
    public R<Set<PermissionDTO>> getMemberPermissions(@PathVariable Long memberId) {
        GetMemberPermissionsQuery query = GetMemberPermissionsQuery.builder()
                .memberId(memberId)
                .build();
        Set<PermissionDTO> permissions = memberPermissionApplicationService.getMemberPermissions(query);
        return R.success(permissions);
    }

    /**
     * Lock a member.
     */
    @PostMapping("/{memberId}/lock")
    public R<Void> lockMember(@PathVariable Long memberId) {
        memberApplicationService.lockMember(memberId);
        return R.success();
    }

    /**
     * Unlock a member.
     */
    @PostMapping("/{memberId}/unlock")
    public R<Void> unlockMember(@PathVariable Long memberId) {
        memberApplicationService.unlockMember(memberId);
        return R.success();
    }
}
