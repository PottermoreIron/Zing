package com.pot.auth.domain.port;

import com.pot.auth.domain.authentication.valueobject.JwtToken;
import com.pot.auth.domain.authentication.valueobject.RefreshToken;
import com.pot.auth.domain.authentication.valueobject.TokenPair;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;

import java.util.Set;

/**
 * Token管理端口接口（防腐层）
 *
 * <p>领域层通过此接口管理JWT Token，不依赖具体的安全框架
 * <p>实现类：
 * <ul>
 *   <li>SpringSecurityJwtAdapter - 基于Spring Security + JJWT实现</li>
 * </ul>
 *
 * @author yecao
 * @since 2025-11-10
 */
public interface TokenManagementPort {

    /**
     * 生成Token对（AccessToken + RefreshToken）
     *
     * @param userId      用户ID
     * @param userDomain  用户域
     * @param username    用户名
     * @param authorities 权限集合
     * @return Token对
     */
    TokenPair generateTokenPair(
            UserId userId,
            UserDomain userDomain,
            String username,
            Set<String> authorities
    );

    /**
     * 解析AccessToken
     *
     * @param tokenString Token字符串
     * @return JwtToken
     */
    JwtToken parseAccessToken(String tokenString);

    /**
     * 解析RefreshToken
     *
     * @param tokenString Token字符串
     * @return RefreshToken
     */
    RefreshToken parseRefreshToken(String tokenString);
}