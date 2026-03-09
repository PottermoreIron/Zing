package com.pot.gateway.filter;

import com.pot.gateway.config.GatewayProperties;
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

/**
 * 统一鉴权过滤器
 *
 * <p>
 * 在网关层完成以下安全校验：
 * <ol>
 * <li>拦截内部服务路径（{@code /internal/**}），直接返回 403</li>
 * <li>白名单路径透传，不验证 Token</li>
 * <li>解析并验证 JWT AccessToken（RSA 公钥签名验证）</li>
 * <li>校验权限版本号：若 Redis 中版本 > Token 中版本，说明权限已变更，拒绝请求</li>
 * <li>向下游注入用户信息
 * Header：{@code X-User-Id}、{@code X-Perm-Version}、{@code X-Perm-Digest}</li>
 * </ol>
 *
 * <p>
 * 优先级：{@code -100}，在 TraceId 过滤器（{@code -200}）之后执行。
 *
 * @author pot
 * @since 2026-03-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationGatewayFilter implements GlobalFilter, Ordered {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PublicKey jwtPublicKey;
    private final GatewayProperties gatewayProperties;

    /** Redis 权限版本号 key 前缀，格式：auth:perm:version:{domain}:{userId} */
    private static final String PERM_VERSION_KEY_PREFIX = "auth:perm:version:";

    // ----------------------------------------------------------------
    // 主流程
    // ----------------------------------------------------------------

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. 拦截内部服务路径，外网禁止直接访问
        if (isInternalPath(path)) {
            log.warn("[鉴权] 禁止访问内部路径: path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        // 2. 白名单路径直接放行
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 3. 提取 Token
        String token = extractToken(exchange);
        if (token == null) {
            log.warn("[鉴权] 未提供 Token: path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            // 4. 验证并解析 Token
            Claims claims = parseToken(token);
            String userId = claims.getSubject();
            Long permVersion = claims.get("perm_version", Long.class);
            String permDigest = claims.get("perm_digest", String.class);
            String userDomain = claims.get("user_domain", String.class);

            // 5. 验证权限版本号（防止权限变更后 Token 中版本已过期）
            if (!isPermVersionValid(userId, userDomain, permVersion)) {
                log.warn("[鉴权] 权限版本已过期，请重新登录: userId={}, tokenVersion={}", userId, permVersion);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // 6. 向下游注入用户信息
            ServerWebExchange enrichedExchange = injectUserHeaders(exchange, userId, userDomain, permVersion,
                    permDigest);

            log.debug("[鉴权] 验证通过: userId={}, domain={}, path={}", userId, userDomain, path);
            return chain.filter(enrichedExchange);

        } catch (Exception e) {
            log.warn("[鉴权] Token 验证失败: path={}, error={}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    // ----------------------------------------------------------------
    // 工具方法
    // ----------------------------------------------------------------

    /**
     * 是否属于内部服务路径（禁止外部访问）
     */
    private boolean isInternalPath(String path) {
        return gatewayProperties.getInternalPathPrefixes().stream()
                .anyMatch(path::startsWith);
    }

    /**
     * 是否在鉴权白名单中
     */
    private boolean isWhiteListed(String path) {
        return gatewayProperties.getWhiteList().stream()
                .anyMatch(path::startsWith);
    }

    /**
     * 从 Authorization Header 提取 Bearer Token
     */
    private String extractToken(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return null;
    }

    /**
     * 使用 RSA 公钥验证并解析 JWT
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(jwtPublicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验权限版本号
     *
     * <p>
     * 若 Redis 中不存在版本号（用户首次缓存或 Redis 清空），则认为有效，
     * 让请求通过，等待下次权限操作时重新写入版本。
     *
     * @param userId       用户 ID
     * @param userDomain   用户域（member / admin）
     * @param tokenVersion Token 中携带的版本号
     * @return true 表示版本有效
     */
    private boolean isPermVersionValid(String userId, String userDomain, Long tokenVersion) {
        if (tokenVersion == null) {
            return true;
        }
        String domain = (userDomain != null) ? userDomain.toLowerCase() : "member";
        String key = PERM_VERSION_KEY_PREFIX + domain + ":" + userId;
        Object currentVersionObj = redisTemplate.opsForValue().get(key);
        if (currentVersionObj == null) {
            return true; // Redis 无记录，视为合法
        }
        long currentVersion = Long.parseLong(currentVersionObj.toString());
        return currentVersion <= tokenVersion;
    }

    /**
     * 向转发请求注入用户信息 Header
     */
    private ServerWebExchange injectUserHeaders(
            ServerWebExchange exchange,
            String userId,
            String userDomain,
            Long permVersion,
            String permDigest) {
        return exchange.mutate()
                .request(r -> r
                        .header("X-User-Id", userId)
                        .header("X-User-Domain", userDomain != null ? userDomain : "")
                        .header("X-Perm-Version", permVersion != null ? permVersion.toString() : "")
                        .header("X-Perm-Digest", permDigest != null ? permDigest : ""))
                .build();
    }
}
