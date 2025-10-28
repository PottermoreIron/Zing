package com.pot.auth.service.service.v1.impl;

import com.pot.auth.service.dto.request.TokenRefreshRequest;
import com.pot.auth.service.dto.request.TokenRevokeRequest;
import com.pot.auth.service.dto.request.TokenValidateRequest;
import com.pot.auth.service.dto.response.TokenValidationResponse;
import com.pot.auth.service.dto.v1.session.AuthSession;
import com.pot.auth.service.service.v1.TokenService;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.security.core.userdetails.SecurityUser;
import com.pot.zing.framework.security.jwt.JwtTokenProvider;
import com.pot.zing.framework.security.jwt.JwtTokenStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Token服务实现
 * <p>
 * 提供Token的刷新、撤销、验证等功能
 *
 * @author Zing
 * @since 2025-10-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenStore jwtTokenStore;
    private final com.pot.auth.service.security.MemberUserDetailsService userDetailsService;

    /**
     * 刷新Token
     */
    public AuthSession refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        try {
            // 1. 验证RefreshToken格式和签名
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                throw new BusinessException("刷新令牌无效或已过期");
            }

            // 2. 验证Token类型
            if (!jwtTokenProvider.validateTokenType(refreshToken, JwtTokenProvider.TokenType.REFRESH)) {
                throw new BusinessException("令牌类型错误");
            }

            // 3. 从Token中获取用户ID
            Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
            if (userId == null) {
                throw new BusinessException("无法获取用户信息");
            }

            // 4. 验证RefreshToken是否与存储的一致（防止重放攻击）
            String storedRefreshToken = jwtTokenStore.getRefreshToken(userId);
            if (!refreshToken.equals(storedRefreshToken)) {
                log.warn("[TokenService] RefreshToken不匹配, userId={}", userId);
                throw new BusinessException("刷新令牌不匹配，请重新登录");
            }

            // 5. 检查是否在黑名单中
            if (isTokenBlacklisted(refreshToken)) {
                throw new BusinessException("刷新令牌已被撤销");
            }

            // 6. 加载用户信息
            UserDetails userDetails = userDetailsService.loadUserById(userId);
            SecurityUser user = (SecurityUser) userDetails;

            // 7. 生成新的AccessToken
            String newAccessToken = jwtTokenProvider.generateAccessToken(user);

            // 8. 生成新的SessionId
            String sessionId = generateSessionId();

            // 9. 记录在线用户
            long accessTokenValidity = jwtTokenProvider.getSecurityProperties().getJwt().getAccessTokenValidity();
            jwtTokenStore.recordOnlineUser(user.getUserId(), newAccessToken, accessTokenValidity);

            // 10. 构建响应
            return AuthSession.builder()
                    .sessionId(sessionId)
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(accessTokenValidity / 1000)
                    .refreshExpiresIn(jwtTokenProvider.getSecurityProperties().getJwt().getRefreshTokenValidity() / 1000)
                    .userInfo(buildUserInfo(user))
                    .isNewUser(false)
                    .authMethod("refresh_token")
                    .build();

        } catch (ExpiredJwtException e) {
            log.warn("[TokenService] RefreshToken已过期");
            throw new BusinessException("刷新令牌已过期，请重新登录");
        } catch (JwtException e) {
            log.error("[TokenService] RefreshToken验证失败", e);
            throw new BusinessException("刷新令牌验证失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[TokenService] 刷新Token失败", e);
            throw new BusinessException("刷新Token失败: " + e.getMessage());
        }
    }

    /**
     * 撤销Token
     */
    public void revokeToken(TokenRevokeRequest request) {
        String token = request.getToken();
        String tokenType = request.getTokenType();

        try {
            // 1. 验证Token格式
            if (!jwtTokenProvider.validateToken(token)) {
                throw new BusinessException("令牌无效");
            }

            // 2. 获取Token过期时间
            Date expiration = jwtTokenProvider.getExpirationFromToken(token);
            long ttl = expiration.getTime() - System.currentTimeMillis();

            if (ttl <= 0) {
                log.info("[TokenService] Token已过期，无需撤销");
                return;
            }

            // 3. 加入黑名单
            addToBlacklist(token, Duration.ofMillis(ttl));

            // 4. 如果是AccessToken，还需要从在线用户列表中移除
            if ("access_token".equals(tokenType)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                if (userId != null) {
                    jwtTokenStore.removeOnlineUser(userId);
                }
            }

            // 5. 如果是RefreshToken，需要删除存储的RefreshToken
            if ("refresh_token".equals(tokenType)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                if (userId != null) {
                    jwtTokenStore.deleteRefreshToken(userId);
                }
            }

            log.info("[TokenService] Token已撤销, tokenType={}, reason={}",
                    tokenType, request.getReason());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[TokenService] 撤销Token失败", e);
            throw new BusinessException("撤销Token失败: " + e.getMessage());
        }
    }

    /**
     * 验证Token
     */
    public TokenValidationResponse validateToken(TokenValidateRequest request) {
        String token = request.getToken();

        try {
            // 1. 检查是否在黑名单中
            if (isTokenBlacklisted(token)) {
                return TokenValidationResponse.builder()
                        .valid(false)
                        .status("REVOKED")
                        .errorMessage("令牌已被撤销")
                        .build();
            }

            // 2. 验证Token
            if (!jwtTokenProvider.validateToken(token)) {
                return TokenValidationResponse.builder()
                        .valid(false)
                        .status("INVALID")
                        .errorMessage("令牌无效或已过期")
                        .build();
            }

            // 3. 提取Token信息
            Claims claims = jwtTokenProvider.getClaimsFromToken(token);
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);
            Date issuedAt = claims.getIssuedAt();
            Date expiration = claims.getExpiration();

            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            @SuppressWarnings("unchecked")
            List<String> permissions = claims.get("permissions", List.class);

            // 4. 计算剩余有效时间
            long expiresIn = (expiration.getTime() - System.currentTimeMillis()) / 1000;

            // 5. 构建响应
            TokenValidationResponse.TokenValidationResponseBuilder builder = TokenValidationResponse.builder()
                    .valid(true)
                    .status("ACTIVE")
                    .userId(userId)
                    .username(username)
                    .tokenType("Bearer")
                    .issuedAt(toLocalDateTime(issuedAt))
                    .expiresAt(toLocalDateTime(expiration))
                    .expiresIn(expiresIn)
                    .roles(roles)
                    .permissions(permissions);

            // 6. 如果需要详细信息，添加额外字段
            if (Boolean.TRUE.equals(request.getIncludeDetails())) {
                // 可以添加更多详细信息，如设备信息、IP等
            }

            return builder.build();

        } catch (ExpiredJwtException e) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .status("EXPIRED")
                    .errorMessage("令牌已过期")
                    .build();
        } catch (JwtException e) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .status("INVALID")
                    .errorMessage("令牌格式错误")
                    .build();
        } catch (Exception e) {
            log.error("[TokenService] 验证Token失败", e);
            return TokenValidationResponse.builder()
                    .valid(false)
                    .status("INVALID")
                    .errorMessage("验证失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 撤销用户的所有Token
     */
    public void revokeUserAllTokens(Long userId) {
        try {
            // 1. 删除RefreshToken
            jwtTokenStore.deleteRefreshToken(userId);

            // 2. 从在线用户列表中移除
            jwtTokenStore.removeOnlineUser(userId);

            // 3. 获取用户的所有会话并加入黑名单
            // 注意：这里需要一个存储机制来追踪用户的所有Token
            // 简化实现：直接清理Redis中的相关数据

            log.info("[TokenService] 已撤销用户所有Token, userId={}", userId);

        } catch (Exception e) {
            log.error("[TokenService] 撤销用户所有Token失败, userId={}", userId, e);
            throw new BusinessException("撤销用户Token失败");
        }
    }

    /**
     * 将Token加入黑名单
     */
    private void addToBlacklist(String token, Duration ttl) {
        String key = "token:blacklist:" + token;
        jwtTokenStore.set(key, "1", ttl.toMillis());
    }

    /**
     * 检查Token是否在黑名单中
     */
    private boolean isTokenBlacklisted(String token) {
        String key = "token:blacklist:" + token;
        return jwtTokenStore.exists(key);
    }

    /**
     * 生成SessionId
     */
    private String generateSessionId() {
        return "session_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 构建用户信息
     */
    private AuthSession.UserInfo buildUserInfo(SecurityUser user) {
        return AuthSession.UserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .roles(user.getAuthorities().stream()
                        .map(Object::toString)
                        .collect(Collectors.toList()))
                .permissions(user.getPermissions())
                .build();
    }

    /**
     * Date转LocalDateTime
     */
    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}

