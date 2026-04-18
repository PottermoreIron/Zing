package com.pot.gateway.filter;

import com.pot.gateway.config.GatewayProperties;
import com.pot.zing.framework.common.util.JacksonUtils;
import com.pot.zing.framework.common.enums.ResultCode;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.redis.service.RedisService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    private static final String DEFAULT_USER_DOMAIN = "member";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_DOMAIN = "X-User-Domain";
    private static final String HEADER_PERMISSION_VERSION = "X-Perm-Version";
    private static final String HEADER_PERMISSION_DIGEST = "X-Perm-Digest";
    private static final String CLAIM_PERMISSION_VERSION = "perm_version";
    private static final String CLAIM_PERMISSION_DIGEST = "perm_digest";
    private static final String CLAIM_USER_DOMAIN = "user_domain";

    private final RedisService redisService;
    private final PublicKey jwtPublicKey;
    private final GatewayProperties gatewayProperties;

    /** Redis key prefix for cached permission versions. */
    private static final String PERM_VERSION_KEY_PREFIX = "auth:perm:version:";

    /** Redis key prefix for revoked access tokens. */
    private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isInternalPath(path)) {
            log.warn("[Auth] Access to internal path is forbidden — path={}", path);
            return reject(exchange, HttpStatus.FORBIDDEN);
        }

        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange);
        if (token == null) {
            log.warn("[Auth] Token not provided — path={}", path);
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        AuthenticatedPrincipal principal;
        try {
            principal = authenticate(token);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("[Auth] Token validation failed — path={}, error={}", path, ex.getMessage());
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        if (isBlacklisted(principal.tokenId())) {
            log.warn("[Auth] Token has been revoked (blacklisted) — tokenId={}", principal.tokenId());
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        if (!isPermVersionValid(principal.userId(), principal.userDomain(), principal.permVersion())) {
            log.warn("[Auth] Permission version expired, please sign in again — userId={}, tokenVersion={}",
                    principal.userId(),
                    principal.permVersion());
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        ServerWebExchange enrichedExchange = injectUserHeaders(exchange, principal);
        log.debug("[Auth] Validation passed — userId={}, domain={}, path={}", principal.userId(),
                principal.userDomain(), path);
        return chain.filter(enrichedExchange);
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

    private AuthenticatedPrincipal authenticate(String token) {
        Claims claims = parseToken(token);
        String userId = claims.getSubject();
        if (userId == null || userId.isBlank()) {
            throw new JwtException("Token subject is missing");
        }

        Long permVersion = claims.get(CLAIM_PERMISSION_VERSION, Long.class);
        String permDigest = claims.get(CLAIM_PERMISSION_DIGEST, String.class);
        // Reject tokens that carry a permission version but no digest; they are
        // structurally inconsistent and should not be trusted.
        if (permVersion != null && (permDigest == null || permDigest.isBlank())) {
            throw new JwtException("Token contains permVersion but is missing permDigest");
        }
        String userDomain = normalizeUserDomain(claims.get(CLAIM_USER_DOMAIN, String.class));
        String tokenId = claims.getId();
        return new AuthenticatedPrincipal(userId, userDomain, permVersion, permDigest, tokenId);
    }

    private boolean isBlacklisted(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisService.exists(BLACKLIST_KEY_PREFIX + tokenId));
        } catch (Exception e) {
            // Fail-closed: treat the token as blacklisted when the revocation store is
            // unreachable. This is preferable to letting potentially revoked tokens
            // through.
            log.error("[Auth] Blacklist check failed — treating token as revoked, tokenId={}, error={}",
                    tokenId, e.getMessage());
            return true;
        }
    }

    private boolean isPermVersionValid(String userId, String userDomain, Long tokenVersion) {
        if (tokenVersion == null) {
            return true;
        }
        String key = PERM_VERSION_KEY_PREFIX + userDomain + ":" + userId;
        String currentVersionStr = redisService.get(key, String.class);
        if (currentVersionStr == null) {
            // Allow requests to continue when the cache has not been populated yet.
            return true;
        }
        long currentVersion = Long.parseLong(currentVersionStr);
        return currentVersion <= tokenVersion;
    }

    private String normalizeUserDomain(String userDomain) {
        if (userDomain == null || userDomain.isBlank()) {
            return DEFAULT_USER_DOMAIN;
        }
        return userDomain.toLowerCase();
    }

    private ServerWebExchange injectUserHeaders(ServerWebExchange exchange, AuthenticatedPrincipal principal) {
        return exchange.mutate()
                .request(r -> r
                        .header(HEADER_USER_ID, principal.userId())
                        .header(HEADER_USER_DOMAIN, principal.userDomain())
                        .header(HEADER_PERMISSION_VERSION,
                                principal.permVersion() != null ? principal.permVersion().toString() : "")
                        .header(HEADER_PERMISSION_DIGEST, principal.permDigest() != null ? principal.permDigest() : ""))
                .build();
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        ResultCode resultCode = switch (status) {
            case UNAUTHORIZED -> ResultCode.UNAUTHORIZED;
            case FORBIDDEN -> ResultCode.FORBIDDEN;
            default -> ResultCode.INTERNAL_ERROR;
        };
        try {
            byte[] body = JacksonUtils.toBytes(R.fail(resultCode));
            exchange.getResponse().setStatusCode(status);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("[Auth] Failed to write rejection response", e);
            exchange.getResponse().setStatusCode(status);
            return exchange.getResponse().setComplete();
        }
    }

    private record AuthenticatedPrincipal(String userId, String userDomain, Long permVersion, String permDigest,
            String tokenId) {
    }
}
