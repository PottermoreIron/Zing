package com.pot.auth.service.service.v1;

import com.pot.auth.service.dto.request.TokenRefreshRequest;
import com.pot.auth.service.dto.request.TokenRevokeRequest;
import com.pot.auth.service.dto.request.TokenValidateRequest;
import com.pot.auth.service.dto.response.TokenValidationResponse;
import com.pot.auth.service.dto.v1.session.AuthSession;

/**
 * Token服务接口
 * <p>
 * 提供Token的刷新、撤销、验证等功能
 *
 * @author Zing
 * @since 2025-10-26
 */
public interface TokenService {

    /**
     * 刷新Token
     *
     * @param request 刷新请求
     * @return 新的认证会话
     */
    AuthSession refreshToken(TokenRefreshRequest request);

    /**
     * 撤销Token
     *
     * @param request 撤销请求
     */
    void revokeToken(TokenRevokeRequest request);

    /**
     * 验证Token
     *
     * @param request 验证请求
     * @return 验证结果
     */
    TokenValidationResponse validateToken(TokenValidateRequest request);

    /**
     * 撤销用户的所有Token
     *
     * @param userId 用户ID
     */
    void revokeUserAllTokens(Long userId);
}

