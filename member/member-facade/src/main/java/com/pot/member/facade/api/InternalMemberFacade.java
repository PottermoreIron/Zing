package com.pot.member.facade.api;

import com.pot.member.facade.dto.DeviceDTO;
import com.pot.member.facade.dto.MemberDTO;
import com.pot.member.facade.dto.MemberProfileDTO;
import com.pot.member.facade.dto.RoleDTO;
import com.pot.member.facade.dto.request.BindSocialAccountRequest;
import com.pot.member.facade.dto.request.CreateMemberRequest;
import com.pot.zing.framework.common.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 会员服务内部 RPC 接口（DDD 统一入口）
 *
 * <p>
 * <b>设计原则</b>：
 * <ul>
 * <li>所有服务间调用统一走 {@code /internal/member} 路径</li>
 * <li>接口方法全部对应 {@code UserModulePort}，保持契约完整</li>
 * <li>只暴露 auth-service / admin-service 等外部系统实际需要的能力</li>
 * <li>DTO 字段最小化暴露，不含敏感信息（如密码哈希）</li>
 * </ul>
 *
 * @author Pot
 * @since 2026-03-18
 */
@FeignClient(name = "member-service", path = "/internal/member")
public interface InternalMemberFacade {

        // ========== 用户查询 ==========

        @GetMapping("/{memberId}")
        R<MemberDTO> findById(@PathVariable("memberId") Long memberId);

        @GetMapping("/by-email")
        R<MemberDTO> findByEmail(@RequestParam("email") String email);

        @GetMapping("/by-phone")
        R<MemberDTO> findByPhone(@RequestParam("phone") String phone);

        @GetMapping("/by-nickname")
        R<MemberDTO> findByNickname(@RequestParam("nickname") String nickname);

        @GetMapping("/by-oauth2")
        R<MemberDTO> findByOAuth2(@RequestParam("provider") String provider,
                        @RequestParam("openId") String openId);

        @GetMapping("/by-wechat")
        R<MemberDTO> findByWeChat(@RequestParam("weChatOpenId") String weChatOpenId);

        // ========== 唯一性检查 ==========

        @GetMapping("/exists/nickname")
        R<Boolean> existsByNickname(@RequestParam("nickname") String nickname);

        @GetMapping("/exists/email")
        R<Boolean> existsByEmail(@RequestParam("email") String email);

        @GetMapping("/exists/phone")
        R<Boolean> existsByPhone(@RequestParam("phone") String phone);

        // ========== 用户创建 ==========

        @PostMapping
        R<MemberDTO> createMember(@RequestBody CreateMemberRequest request);

        // ========== 认证 ==========

        @PostMapping("/auth/verify-password")
        R<MemberDTO> authenticateWithPassword(@RequestParam("identifier") String identifier,
                        @RequestParam("password") String password);

        // ========== 密码管理 ==========

        @PutMapping("/{memberId}/password")
        R<Void> updatePassword(@PathVariable("memberId") Long memberId,
                        @RequestParam("newPasswordHash") String newPasswordHash);

        // ========== 账户管理 ==========

        @PutMapping("/{memberId}/lock")
        R<Void> lockAccount(@PathVariable("memberId") Long memberId);

        @PutMapping("/{memberId}/unlock")
        R<Void> unlockAccount(@PathVariable("memberId") Long memberId);

        @PostMapping("/{memberId}/login-attempt")
        R<Void> recordLoginAttempt(@PathVariable("memberId") Long memberId,
                        @RequestParam("success") boolean success,
                        @RequestParam("ip") String ip,
                        @RequestParam("timestamp") Long timestamp);

        // ========== 权限查询 ==========

        @GetMapping("/{memberId}/permissions")
        R<Set<String>> getPermissions(@PathVariable("memberId") Long memberId);

        @GetMapping("/{memberId}/roles")
        R<List<RoleDTO>> getRoles(@PathVariable("memberId") Long memberId);

        @PostMapping("/permissions/batch")
        R<Map<Long, Set<String>>> getPermissionsBatch(@RequestBody List<Long> memberIds);

        // ========== 设备管理 ==========

        @GetMapping("/{memberId}/devices")
        R<List<DeviceDTO>> getDevices(@PathVariable("memberId") Long memberId);

        @PostMapping("/{memberId}/devices")
        R<Void> recordDeviceLogin(@PathVariable("memberId") Long memberId,
                        @RequestBody DeviceDTO device,
                        @RequestParam("ip") String ip,
                        @RequestParam("refreshToken") String refreshToken);

        @DeleteMapping("/{memberId}/devices/{deviceId}")
        R<Void> kickDevice(@PathVariable("memberId") Long memberId,
                        @PathVariable("deviceId") Long deviceId);

        // ========== OAuth2 / 社交账号绑定 ==========

        @PostMapping("/{memberId}/oauth2")
        R<Void> bindOAuth2(@PathVariable("memberId") Long memberId,
                        @RequestBody BindSocialAccountRequest request);

        // ========== Profile ==========

        @GetMapping("/{memberId}/profile")
        R<MemberProfileDTO> getProfile(@PathVariable("memberId") Long memberId);
}
