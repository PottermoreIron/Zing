package com.pot.auth.infrastructure.client;

import com.pot.member.facade.dto.MemberDTO;
import com.pot.member.facade.dto.request.CreateMemberRequest;
import com.pot.zing.framework.common.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Member服务的Feign Client（防腐层设计）
 *
 * <p><strong>设计原则</strong>：
 * <ul>
 *   <li>❌ 不继承MemberFacade - 避免与member-service强绑定</li>
 *   <li>✅ 只定义auth-service实际需要的方法 - 按需定义</li>
 *   <li>✅ 使用member-facade的DTO - 复用数据契约</li>
 *   <li>✅ MemberModuleAdapter负责DTO转换 - 隔离变更</li>
 * </ul>
 *
 * <p><strong>防腐层价值</strong>：
 * <ul>
 *   <li>member-facade新增方法不影响auth-service</li>
 *   <li>可以轻松添加AdminServiceClient、MerchantServiceClient</li>
 *   <li>MemberModuleAdapter统一适配不同的Client实现</li>
 * </ul>
 *
 * @author pot
 * @since 1.0.0
 */
@FeignClient(name = "member-service", path = "/member")
public interface MemberServiceClient {

    // ========== 认证相关 ==========

    /**
     * 根据用户名获取用户（用于密码登录）
     */
    @GetMapping("/getByUsername")
    R<MemberDTO> getMemberByUsername(@RequestParam("username") String username);

    /**
     * 根据邮箱获取用户（用于密码登录）
     */
    @GetMapping("/getByEmail")
    R<MemberDTO> getMemberByEmail(@RequestParam("email") String email);

    /**
     * 根据手机号获取用户（用于密码登录）
     */
    @GetMapping("/getByPhone")
    R<MemberDTO> getMemberByPhone(@RequestParam("phone") String phone);

    /**
     * 根据用户ID获取用户
     */
    @GetMapping("/getById")
    R<MemberDTO> getMemberById(@RequestParam("memberId") Long memberId);

    // ========== 注册相关 ==========

    /**
     * 创建新用户
     */
    @PostMapping("/create")
    R<MemberDTO> createMember(@RequestBody CreateMemberRequest request);

    /**
     * 检查邮箱是否已存在
     */
    @GetMapping("/checkEmailExists")
    R<Boolean> checkEmailExists(@RequestParam("email") String email);

    /**
     * 检查手机号是否已存在
     */
    @GetMapping("/checkPhoneExists")
    R<Boolean> checkPhoneExists(@RequestParam("phone") String phone);

    // ========== OAuth2相关 ==========

    /**
     * 根据OAuth2信息查询用户
     */
    @GetMapping("/getByOAuth2")
    R<MemberDTO> getMemberByOAuth2(
            @RequestParam("provider") String provider,
            @RequestParam("openId") String openId
    );

    /**
     * 从OAuth2信息创建新用户
     */
    @PostMapping("/createFromOAuth2")
    R<MemberDTO> createMemberFromOAuth2(
            @RequestParam("provider") String provider,
            @RequestParam("openId") String openId,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "avatarUrl", required = false) String avatarUrl
    );

    /**
     * 根据微信UnionID查询用户
     */
    @GetMapping("/getByUnionId")
    R<MemberDTO> getMemberByUnionId(@RequestParam("unionId") String unionId);

    /**
     * 绑定OAuth2账号到已有用户
     */
    @PostMapping("/bindOAuth2Account")
    R<Void> bindOAuth2Account(
            @RequestParam("memberId") Long memberId,
            @RequestParam("provider") String provider,
            @RequestParam("openId") String openId
    );

    // ========== 权限相关（未来member-service提供内部API时添加）==========

    // TODO: 等member-service提供权限查询内部API后添加以下方法
    // R<Set<String>> getPermissions(@RequestParam("memberId") Long memberId);
    // R<Set<RoleDTO>> getRoles(@RequestParam("memberId") Long memberId);

    // ========== 设备管理（未来member-service提供内部API时添加）==========

    // TODO: 等member-service提供设备管理内部API后添加以下方法
    // R<List<DeviceDTO>> getDevices(@RequestParam("memberId") Long memberId);
    // R<Void> recordDeviceLogin(@RequestParam("memberId") Long memberId, @RequestBody DeviceLoginRequest request);
}

