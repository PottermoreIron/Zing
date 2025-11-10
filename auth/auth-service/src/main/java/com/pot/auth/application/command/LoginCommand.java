package com.pot.auth.application.command;

import com.pot.auth.domain.shared.valueobject.UserDomain;

/**
 * 登录命令
 *
 * @author yecao
 * @since 2025-11-10
 */
public record LoginCommand(
        String identifier,      // 用户标识（用户名/邮箱/手机号）
        String password,        // 密码
        UserDomain userDomain,  // 用户域
        String ipAddress,       // IP地址
        String userAgent        // User-Agent
) {
    /**
     * 构造函数参数校验
     *
     * @param identifier 用户标识
     * @param password   密码
     * @param userDomain 用户域
     * @param ipAddress  IP地址
     * @param userAgent  User-Agent
     */
    public LoginCommand {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("用户标识不能为空");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (userDomain == null) {
            throw new IllegalArgumentException("用户域不能为空");
        }
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new IllegalArgumentException("IP地址不能为空");
        }
    }

    /**
     * 静态工厂方法
     *
     * @param identifier 用户标识
     * @param password   密码
     * @param userDomain 用户域
     * @param ipAddress  IP地址
     * @param userAgent  User-Agent
     * @return 登录命令实例
     */
    public static LoginCommand of(
            String identifier,
            String password,
            UserDomain userDomain,
            String ipAddress,
            String userAgent
    ) {
        return new LoginCommand(identifier, password, userDomain, ipAddress, userAgent);
    }
}

