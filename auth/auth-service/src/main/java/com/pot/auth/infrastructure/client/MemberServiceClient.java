package com.pot.auth.infrastructure.client;

import com.pot.member.facade.dto.MemberDTO;
import com.pot.member.facade.dto.request.CreateMemberRequest;
import com.pot.zing.framework.common.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.Set;

/**
 * Member服务的Feign Client（防腐层设计）
 *
 * <p>
 * <strong>设计原则</strong>：
 * <ul>
 * <li>❌ 不继承MemberFacade - 避免与member-service强绑定</li>
 * <li>✅ 只定义auth-service实际需要的方法 - 按需定义</li>
 * <li>✅ 使用member-facade的DTO - 复用数据契约</li>
 * <li>✅ MemberModuleAdapter负责DTO转换 - 隔离变更</li>
 * </ul>
 *
 * <p>
 * <strong>防腐层价值</strong>：
 * <ul>
 * <li>member-facade新增方法不影响auth-service</li>
 * <li>可以轻松添加AdminServiceClient、MerchantServiceClient</li>
 * <li>MemberModuleAdapter统一适配不同的Client实现</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
@FeignClient(name = "member-service", path = "/member")
public interface MemberServiceClient {

        // ========== 认证相关 ==========

        /**
         * 验证用户密码（内部API）
         */
        @PostMapping("/internal/auth/verify-password")
        R<Boolean> verifyPassword(
                        @RequestParam("identifier") String identifier,
                        @RequestParam("password") String password);

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
                        @RequestParam("openId") String openId);

        /**
         * 从OAuth2信息创建新用户
         */
        @PostMapping("/createFromOAuth2")
        R<MemberDTO> createMemberFromOAuth2(
                        @RequestParam("provider") String provider,
                        @RequestParam("openId") String openId,
                        @RequestParam(value = "email", required = false) String email,
                        @RequestParam(value = "nickname", required = false) String nickname,
                        @RequestParam(value = "avatarUrl", required = false) String avatarUrl);

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
                        @RequestParam("openId") String openId);

        // ========== 账户管理 ==========

        /**
         * 记录登录尝试
         */
        @PostMapping("/internal/auth/login-attempt")
        R<Void> recordLoginAttempt(
                        @RequestParam("userId") String userId,
                        @RequestParam("success") Boolean success,
                        @RequestParam("ip") String ip);

        /**
         * 锁定账户
         */
        @PutMapping("/internal/auth/lock/{userId}")
        R<Void> lockAccount(@PathVariable("userId") String userId);

        /**
         * 解锁账户
         */
        @PutMapping("/internal/auth/unlock/{userId}")
        R<Void> unlockAccount(@PathVariable("userId") String userId);

        // ========== 权限相关 ==========

        /**
         * 查询用户权限集合
         */
        @GetMapping("/internal/member/{userId}/permissions")
        R<Set<String>> getPermissions(@PathVariable("userId") String userId);

        /**
         * 查询用户角色集合
         */
        @GetMapping("/internal/member/{userId}/roles")
        R<Set<String>> getRoles(@PathVariable("userId") String userId);

        /**
         * 批量查询用户权限
         */
        @PostMapping("/internal/member/permissions/batch")
        R<Map<String, Set<String>>> batchQueryPermissions(@RequestBody Set<String> userIds);
}
