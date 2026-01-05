package com.pot.member.service.controller.internal;

import com.pot.member.service.service.MemberAuthInternalService;
import com.pot.zing.framework.common.model.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 内部认证接口
 *
 * <p>
 * 提供给auth-service的内部接口，用于用户认证和密码验证：
 * <ul>
 * <li>密码验证</li>
 * <li>登录尝试记录</li>
 * <li>账户锁定/解锁</li>
 * </ul>
 *
 * @author Copilot
 * @since 2026-01-05
 */
@Slf4j
@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class InternalAuthController {

    private final MemberAuthInternalService authService;

    /**
     * 验证用户密码
     *
     * @param identifier 用户标识（用户名/邮箱/手机号）
     * @param password   密码明文
     * @return 验证结果（true=验证成功，false=验证失败）
     */
    @PostMapping("/verify-password")
    public R<Boolean> verifyPassword(
            @RequestParam String identifier,
            @RequestParam String password) {
        log.info("[内部接口] 验证密码: identifier={}", identifier);

        boolean isValid = authService.verifyPassword(identifier, password);

        if (isValid) {
            log.info("[内部接口] 密码验证成功: identifier={}", identifier);
            return R.success(true);
        } else {
            log.warn("[内部接口] 密码验证失败: identifier={}", identifier);
            return R.success(false);
        }
    }

    /**
     * 记录登录尝试
     *
     * @param userId  用户ID
     * @param success 是否成功
     * @param ip      IP地址
     * @return 操作结果
     */
    @PostMapping("/login-attempt")
    public R<Void> recordLoginAttempt(
            @RequestParam String userId,
            @RequestParam Boolean success,
            @RequestParam String ip) {
        log.info("[内部接口] 记录登录尝试: userId={}, success={}, ip={}", userId, success, ip);

        authService.recordLoginAttempt(userId, success, ip);

        return R.success();
    }

    /**
     * 锁定账户
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @PutMapping("/lock/{userId}")
    public R<Void> lockAccount(@PathVariable String userId) {
        log.info("[内部接口] 锁定账户: userId={}", userId);

        authService.lockAccount(userId);

        return R.success();
    }

    /**
     * 解锁账户
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @PutMapping("/unlock/{userId}")
    public R<Void> unlockAccount(@PathVariable String userId) {
        log.info("[内部接口] 解锁账户: userId={}", userId);

        authService.unlockAccount(userId);

        return R.success();
    }
}
