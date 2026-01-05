package com.pot.member.service.service;

/**
 * 内部认证服务接口
 *
 * <p>
 * 提供给auth-service的内部认证功能：
 * <ul>
 * <li>密码验证（BCrypt）</li>
 * <li>登录尝试记录和追踪</li>
 * <li>账户锁定/解锁管理</li>
 * </ul>
 *
 * @author Copilot
 * @since 2026-01-05
 */
public interface MemberAuthInternalService {

    /**
     * 验证用户密码
     *
     * @param identifier 用户标识（用户名/邮箱/手机号）
     * @param password   密码明文
     * @return true=验证成功，false=验证失败
     */
    boolean verifyPassword(String identifier, String password);

    /**
     * 记录登录尝试
     *
     * @param userId  用户ID
     * @param success 是否成功
     * @param ip      IP地址
     */
    void recordLoginAttempt(String userId, Boolean success, String ip);

    /**
     * 锁定账户
     *
     * @param userId 用户ID
     */
    void lockAccount(String userId);

    /**
     * 解锁账户
     *
     * @param userId 用户ID
     */
    void unlockAccount(String userId);
}
