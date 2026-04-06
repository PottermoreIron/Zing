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
 * Performs gateway-level access control before requests reach downstream
 * services.
 *
 * <p>
 * The filter blocks internal paths, skips configured public routes, validates
 * the JWT access token, checks the permission version against Redis, and
 * forwards
 * user identity headers to downstream services.
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

    /** Redis key prefix for cached permission versions. */
    private static final String PERM_VERSION_KEY_PREFIX = "auth:perm:version:";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isInternalPath(path)) {
            log.warn("[鉴权] 禁止访问内部路径: path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange);
        if (token == null) {
            log.warn("[鉴权] 未提供 Token: path={}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            Claims claims = parseToken(token);
            String userId = claims.getSubject();
            Long permVersion = claims.get("perm_version", Long.class);
            String permDigest = claims.get("perm_digest", String.class);
            String userDomain = claims.get("user_domain", String.class);

            if (!isPermVersionValid(userId, userDomain, permVersion)) {
                log.warn("[鉴权] 权限版本已过期，请重新登录: userId={}, tokenVersion={}", userId, permVersion);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

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

    private boolean isInternalPath(String path) {
        return gatewayProperties.getInternalPathPrefixes().stream()
                .anyMatch(path::startsWith);
    }

    private boolean isWhiteListed(String path) {
        return gatewayProperties.getWhiteList().stream()
                .anyMatch(path::startsWith);
    }

    private String extractToken(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return null;
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(jwtPublicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isPermVersionValid(String userId, String userDomain, Long tokenVersion) {
        if (tokenVersion == null) {
            return true;
        }
        String domain = (userDomain != null) ? userDomain.toLowerCase() : "member";
        String key = PERM_VERSION_KEY_PREFIX + domain + ":" + userId;
        Object currentVersionObj = redisTemplate.opsForValue().get(key);
        if (currentVersionObj == null) {
            // Allow requests to continue when the cache has not been populated yet.
            return true;
        }
        long currentVersion = Long.parseLong(currentVersionObj.toString());
        return currentVersion <= tokenVersion;
    }

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
