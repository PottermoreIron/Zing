package com.pot.member.service.controller.internal;

import com.pot.member.facade.dto.DeviceDTO;
import com.pot.member.facade.dto.MemberDTO;
import com.pot.member.facade.dto.MemberProfileDTO;
import com.pot.member.facade.dto.RoleDTO;
import com.pot.member.facade.dto.request.BindSocialAccountRequest;
import com.pot.member.facade.dto.request.CreateMemberRequest;
import com.pot.member.service.application.command.RegisterMemberCommand;
import com.pot.member.service.application.query.GetMemberQuery;
import com.pot.member.service.application.service.MemberApplicationService;
import com.pot.zing.framework.common.model.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 会员内部 API 控制器（DDD 统一入口）
 *
 * <p>
 * 路径固定为 {@code /internal/member}，与 {@code InternalMemberFacade} Feign 客户端完全对应。
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

    // ========== 用户查询 ==========

    @GetMapping("/{memberId}")
    public R<MemberDTO> findById(@PathVariable Long memberId) {
        var internalDTO = memberApplicationService.getMember(
                GetMemberQuery.builder().memberId(memberId).build());
        return R.success(toFacadeDTO(internalDTO, memberId));
    }

    @GetMapping("/by-email")
    public R<MemberDTO> findByEmail(@RequestParam String email) {
        var internalDTO = memberApplicationService.getMember(
                GetMemberQuery.builder().email(email).build());
        return R.success(toFacadeDTO(internalDTO, null));
    }

    @GetMapping("/by-phone")
    public R<MemberDTO> findByPhone(@RequestParam String phone) {
        var internalDTO = memberApplicationService.getMember(
                GetMemberQuery.builder().phoneNumber(phone).build());
        return R.success(toFacadeDTO(internalDTO, null));
    }

    @GetMapping("/by-nickname")
    public R<MemberDTO> findByNickname(@RequestParam String nickname) {
        var internalDTO = memberApplicationService.getMember(
                GetMemberQuery.builder().nickname(nickname).build());
        return R.success(toFacadeDTO(internalDTO, null));
    }

    @GetMapping("/by-oauth2")
    public R<MemberDTO> findByOAuth2(@RequestParam String provider, @RequestParam String openId) {
        var internalDTO = memberApplicationService.findByOAuth2(provider, openId);
        return R.success(toFacadeDTO(internalDTO, null));
    }

    @GetMapping("/by-wechat")
    public R<MemberDTO> findByWeChat(@RequestParam String weChatOpenId) {
        var internalDTO = memberApplicationService.findByWeChat(weChatOpenId);
        return R.success(toFacadeDTO(internalDTO, null));
    }

    // ========== 唯一性检查 ==========

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

    // ========== 用户创建 ==========

    @PostMapping
    public R<MemberDTO> createMember(@RequestBody @Valid CreateMemberRequest request) {
        RegisterMemberCommand command = RegisterMemberCommand.builder()
                .nickname(request.getNickname())
                .email(request.getEmail())
                .password(request.getPassword())
                .phoneNumber(request.getPhone())
                .build();
        var internalDTO = memberApplicationService.register(command);
        return R.success(toFacadeDTO(internalDTO, null));
    }

    // ========== 认证 ==========

    @PostMapping("/auth/verify-password")
    public R<MemberDTO> authenticateWithPassword(@RequestParam String identifier,
            @RequestParam String password) {
        var internalDTO = memberApplicationService.authenticateWithPassword(identifier, password);
        return R.success(toFacadeDTO(internalDTO, null));
    }

    // ========== 密码管理 ==========

    @PutMapping("/{memberId}/password")
    public R<Void> updatePassword(@PathVariable Long memberId,
            @RequestParam String newPasswordHash) {
        memberApplicationService.updatePasswordHash(memberId, newPasswordHash);
        return R.success(null);
    }

    // ========== 账户管理 ==========

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

    // ========== 权限查询 ==========

    @GetMapping("/{memberId}/permissions")
    public R<Set<String>> getPermissions(@PathVariable Long memberId) {
        return R.success(memberApplicationService.getPermissionCodes(memberId));
    }

    @GetMapping("/{memberId}/roles")
    public R<List<RoleDTO>> getRoles(@PathVariable Long memberId) {
        return R.success(memberApplicationService.getMemberRoles(memberId));
    }

    @PostMapping("/permissions/batch")
    public R<Map<Long, Set<String>>> getPermissionsBatch(@RequestBody List<Long> memberIds) {
        return R.success(memberApplicationService.getPermissionsBatch(memberIds));
    }

    // ========== 设备管理 ==========

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

    // ========== OAuth2 / 社交账号绑定 ==========

    @PostMapping("/{memberId}/oauth2")
    public R<Void> bindOAuth2(@PathVariable Long memberId,
            @RequestBody @Valid BindSocialAccountRequest request) {
        memberApplicationService.bindOAuth2(memberId, request);
        return R.success(null);
    }

    // ========== Profile ==========

    @GetMapping("/{memberId}/profile")
    public R<MemberProfileDTO> getProfile(@PathVariable Long memberId) {
        return R.success(memberApplicationService.getProfile(memberId));
    }

    // ========== 私有辅助 ==========

    /**
     * 将应用层内部DTO转换为对外facade DTO
     */
    private MemberDTO toFacadeDTO(com.pot.member.service.application.dto.MemberDTO internalDTO, Long memberIdHint) {
        if (internalDTO == null) {
            return null;
        }
        return MemberDTO.builder()
                .memberId(internalDTO.getMemberId())
                .username(internalDTO.getNickname())
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
