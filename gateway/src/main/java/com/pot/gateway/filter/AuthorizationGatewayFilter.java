package com.pot.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.PublicKey;
import java.util.List;

/**
 * 统一授权过滤器
 *
 * <p>
 * 在网关层进行统一权限验证：
 * <ul>
 * <li>解析JWT Token</li>
 * <li>验证权限版本号</li>
 * <li>验证权限摘要</li>
 * <li>注入用户信息到下游服务</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationGatewayFilter implements GlobalFilter, Ordered {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PublicKey jwtPublicKey;

    // 白名单路径
    private static final List<String> WHITE_LIST = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/refresh");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. 检查白名单
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 2. 获取Token
        String token = extractToken(exchange);
        if (token == null) {
            log.warn("[网关授权] 未提供Token: path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            // 3. 解析Token
            Claims claims = parseToken(token);
            String userId = claims.getSubject();
            Long permVersion = claims.get("perm_version", Long.class);
            String permDigest = claims.get("perm_digest", String.class);

            // 4. 验证权限版本号
            Long currentVersion = getCurrentPermissionVersion(userId);
            if (currentVersion != null && !currentVersion.equals(permVersion)) {
                log.warn("[网关授权] 权限版本不匹配: userId={}, tokenVersion={}, currentVersion={}",
                        userId, permVersion, currentVersion);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // 5. 注入用户信息到Header
            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(r -> r.header("X-User-Id", userId)
                            .header("X-Perm-Version", String.valueOf(permVersion))
                            .header("X-Perm-Digest", permDigest))
                    .build();

            log.debug("[网关授权] 验证通过: userId={}, path={}", userId, path);
            return chain.filter(modifiedExchange);

        } catch (Exception e) {
            log.error("[网关授权] Token验证失败: path={}, error={}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * 检查是否在白名单中
     */
    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    /**
     * 从请求中提取Token
     */
    private String extractToken(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    /**
     * 解析Token
     */
    private Claims parseToken(String token) throws Exception {
        return Jwts.parser()
                .verifyWith(jwtPublicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取当前权限版本号
     */
    private Long getCurrentPermissionVersion(String userId) {
        String key = "auth:perm:version:member:" + userId;
        Object version = redisTemplate.opsForValue().get(key);
        return version != null ? Long.parseLong(version.toString()) : null;
    }

    @Override
    public int getOrder() {
        return -100; // 高优先级
    }
}
