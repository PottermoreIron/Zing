package com.pot.member.service.interfaces.rest.internal;

import com.pot.member.facade.dto.DeviceDTO;
import com.pot.member.facade.dto.MemberDTO;
import com.pot.member.facade.dto.MemberProfileDTO;
import com.pot.member.facade.dto.RoleDTO;
import com.pot.member.facade.dto.request.BindSocialAccountRequest;
import com.pot.member.facade.dto.request.CreateMemberRequest;
import com.pot.member.service.application.command.CreateMemberCommand;
import com.pot.member.service.application.query.GetMemberQuery;
import com.pot.member.service.application.service.MemberApplicationService;
import com.pot.member.service.application.service.MemberPermissionApplicationService;
import com.pot.zing.framework.common.model.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Internal REST controller aligned with the member-service facade contract.
 *
 * @author Pot
 * @since 2026-03-18
 */
@Slf4j
@RestController
@RequestMapping("/internal/member")
@RequiredArgsConstructor
public class InternalMemberController {

    private final MemberApplicationService memberApplicationService;
    private final MemberPermissionApplicationService memberPermissionApplicationService;

    @GetMapping("/{memberId}")
    public R<MemberDTO> findById(@PathVariable Long memberId) {
        var internalDTO = memberApplicationService.getMember(
                GetMemberQuery.byMemberId(memberId));
        return R.success(toFacadeDTO(internalDTO));
    }

    @GetMapping("/by-email")
    public R<MemberDTO> findByEmail(@RequestParam String email) {
        var internalDTO = memberApplicationService.getMember(
                GetMemberQuery.byEmail(email));
        return R.success(toFacadeDTO(internalDTO));
    }

    @GetMapping("/by-phone")
    public R<MemberDTO> findByPhone(@RequestParam String phone) {
        var internalDTO = memberApplicationService.getMember(
                GetMemberQuery.byPhoneNumber(phone));
        return R.success(toFacadeDTO(internalDTO));
    }

    @GetMapping("/by-nickname")
    public R<MemberDTO> findByNickname(@RequestParam String nickname) {
        var internalDTO = memberApplicationService.getMember(
                GetMemberQuery.byNickname(nickname));
        return R.success(toFacadeDTO(internalDTO));
    }

    @GetMapping("/by-oauth2")
    public R<MemberDTO> findByOAuth2(@RequestParam String provider, @RequestParam String openId) {
        var internalDTO = memberApplicationService.findByOAuth2(provider, openId);
        return R.success(toFacadeDTO(internalDTO));
    }

    @GetMapping("/by-wechat")
    public R<MemberDTO> findByWeChat(@RequestParam String weChatOpenId) {
        var internalDTO = memberApplicationService.findByWeChat(weChatOpenId);
        return R.success(toFacadeDTO(internalDTO));
    }

    @GetMapping("/exists/nickname")
    public R<Boolean> existsByNickname(@RequestParam String nickname) {
        return R.success(memberApplicationService.existsByNickname(nickname));
    }

    @GetMapping("/exists/email")
    public R<Boolean> existsByEmail(@RequestParam String email) {
        return R.success(memberApplicationService.existsByEmail(email));
    }

    @GetMapping("/exists/phone")
    public R<Boolean> existsByPhone(@RequestParam String phone) {
        return R.success(memberApplicationService.existsByPhone(phone));
    }

    @PostMapping
    public R<MemberDTO> createMember(@RequestBody @Valid CreateMemberRequest request) {
        CreateMemberCommand command = CreateMemberCommand.builder()
                .nickname(request.getNickname())
                .email(request.getEmail())
                .password(request.getPassword())
                .phoneNumber(request.getPhone())
                .build();
        var internalDTO = memberApplicationService.createMember(command);
        return R.success(toFacadeDTO(internalDTO));
    }

    @PostMapping("/auth/verify-password")
    public R<MemberDTO> authenticateWithPassword(@RequestParam String identifier,
            @RequestParam String password) {
        var internalDTO = memberApplicationService.authenticateWithPassword(identifier, password);
        return R.success(toFacadeDTO(internalDTO));
    }

    @PutMapping("/{memberId}/password")
    public R<Void> updatePassword(@PathVariable Long memberId,
            @RequestParam String newPasswordHash) {
        memberApplicationService.updatePasswordHash(memberId, newPasswordHash);
        return R.success(null);
    }

    @PutMapping("/{memberId}/lock")
    public R<Void> lockAccount(@PathVariable Long memberId) {
        memberApplicationService.lockMember(memberId);
        return R.success(null);
    }

    @PutMapping("/{memberId}/unlock")
    public R<Void> unlockAccount(@PathVariable Long memberId) {
        memberApplicationService.unlockMember(memberId);
        return R.success(null);
    }

    @PostMapping("/{memberId}/login-attempt")
    public R<Void> recordLoginAttempt(@PathVariable Long memberId,
            @RequestParam boolean success,
            @RequestParam String ip,
            @RequestParam Long timestamp) {
        memberApplicationService.recordLoginAttempt(memberId, success, ip, timestamp);
        return R.success(null);
    }

    @GetMapping("/{memberId}/permissions")
    public R<Set<String>> getPermissions(@PathVariable Long memberId) {
        return R.success(memberPermissionApplicationService.getPermissionCodes(memberId));
    }

    @GetMapping("/{memberId}/roles")
    public R<List<RoleDTO>> getRoles(@PathVariable Long memberId) {
        return R.success(memberPermissionApplicationService.getMemberRoles(memberId));
    }

    @PostMapping("/permissions/batch")
    public R<Map<Long, Set<String>>> getPermissionsBatch(@RequestBody List<Long> memberIds) {
        return R.success(memberPermissionApplicationService.getPermissionsBatch(memberIds));
    }

    @GetMapping("/{memberId}/devices")
    public R<List<DeviceDTO>> getDevices(@PathVariable Long memberId) {
        return R.success(memberApplicationService.getDevices(memberId));
    }

    @PostMapping("/{memberId}/devices")
    public R<Void> recordDeviceLogin(@PathVariable Long memberId,
            @RequestBody DeviceDTO device,
            @RequestParam String ip,
            @RequestParam String refreshToken) {
        memberApplicationService.recordDeviceLogin(memberId, device, ip, refreshToken);
        return R.success(null);
    }

    @DeleteMapping("/{memberId}/devices/{deviceId}")
    public R<Void> kickDevice(@PathVariable Long memberId,
            @PathVariable Long deviceId) {
        memberApplicationService.kickDevice(memberId, deviceId);
        return R.success(null);
    }

    @PostMapping("/{memberId}/oauth2")
    public R<Void> bindOAuth2(@PathVariable Long memberId,
            @RequestBody @Valid BindSocialAccountRequest request) {
        memberApplicationService.bindOAuth2(memberId, request);
        return R.success(null);
    }

    @GetMapping("/{memberId}/profile")
    public R<MemberProfileDTO> getProfile(@PathVariable Long memberId) {
        return R.success(memberApplicationService.getProfile(memberId));
    }

    private MemberDTO toFacadeDTO(com.pot.member.service.application.dto.MemberDTO internalDTO) {
        if (internalDTO == null) {
            return null;
        }
        return MemberDTO.builder()
                .memberId(internalDTO.getMemberId())
                .nickname(internalDTO.getNickname())
                .email(internalDTO.getEmail())
                .phone(internalDTO.getPhoneNumber())
                .status(internalDTO.getStatus())
                .gmtCreatedAt(internalDTO.getCreatedAt() != null
                        ? internalDTO.getCreatedAt().toEpochSecond(ZoneOffset.UTC) * 1000
                        : null)
                .gmtLastLoginAt(internalDTO.getLastLoginAt() != null
                        ? internalDTO.getLastLoginAt().toEpochSecond(ZoneOffset.UTC) * 1000
                        : null)
                .build();
    }
}
