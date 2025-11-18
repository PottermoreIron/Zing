package com.pot.auth.domain.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;

/**
 * 统一认证策略接口
 *
 * <p>用于OAuth2和微信登录等注册登录一体化的场景
 * <p>这类认证方式的特点是：
 * <ul>
 *   <li>第一次访问时自动注册</li>
 *   <li>后续访问直接登录</li>
 *   <li>无需用户显式选择注册或登录</li>
 * </ul>
 *
 * @author yecao
 * @since 2025-11-18
 */
public interface AuthenticationStrategy {

    /**
     * 执行认证逻辑（自动处理注册或登录）
     *
     * @param provider OAuth2提供商或认证方式标识（如google, github, wechat）
     * @param authorizationCode 授权码
     * @param state 状态参数（可选，用于防CSRF攻击）
     * @param userDomain 用户域
     * @param ipAddress 客户端IP地址
     * @param userAgent 用户代理信息
     * @return 认证结果（包含Token）
     */
    AuthenticationResult authenticate(
            String provider,
            String authorizationCode,
            String state,
            String userDomain,
            String ipAddress,
            String userAgent
    );

    /**
     * 判断该策略是否支持指定的认证方式
     *
     * @param authenticationType 认证类型（如OAUTH2, WECHAT）
     * @return true if支持, false otherwise
     */
    boolean supports(String authenticationType);
}

