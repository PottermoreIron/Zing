package com.pot.auth.domain.port;

import com.pot.auth.domain.wechat.entity.WeChatUserInfo;

/**
 * 微信服务端口
 *
 * <p>
 * 用于与微信开放平台交互，获取用户信息
 *
 * <p>
 * 实现类应该：
 * <ul>
 * <li>调用微信API获取用户信息</li>
 * <li>处理授权码验证</li>
 * <li>处理异常情况（授权码失效、网络错误等）</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-30
 */
public interface WeChatPort {

    /**
     * 通过授权码获取微信用户信息
     *
     * <p>
     * 执行流程：
     * <ol>
     * <li>使用授权码换取 access_token</li>
     * <li>使用 access_token 获取用户信息</li>
     * <li>返回规范化的用户信息对象</li>
     * </ol>
     *
     * @param code  微信授权码（用户同意授权后，微信回调时提供）
     * @param state 状态参数（用于防CSRF攻击，可选）
     * @return 微信用户信息
     * @throws com.pot.auth.domain.shared.exception.DomainException 当授权码无效、已过期或API调用失败时
     */
    WeChatUserInfo getUserInfo(String code, String state);

    /**
     * 刷新访问令牌
     *
     * <p>
     * 当 access_token 过期时，使用 refresh_token 获取新的 access_token
     *
     * @param refreshToken 刷新令牌
     * @return 新的访问令牌
     * @throws com.pot.auth.domain.shared.exception.DomainException 当刷新令牌无效或已过期时
     */
    String refreshAccessToken(String refreshToken);

    /**
     * 验证访问令牌是否有效
     *
     * @param accessToken 访问令牌
     * @return true-有效，false-无效
     */
    boolean validateAccessToken(String accessToken);
}
