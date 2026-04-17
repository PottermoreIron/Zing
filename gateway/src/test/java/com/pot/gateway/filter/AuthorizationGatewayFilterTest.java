package com.pot.gateway.filter;

import com.pot.gateway.config.GatewayProperties;
import com.pot.zing.framework.starter.redis.service.RedisService;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationGatewayFilter")
class AuthorizationGatewayFilterTest {

    @Mock
    private RedisService redisService;

    private AuthorizationGatewayFilter filter;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        GatewayProperties gatewayProperties = new GatewayProperties();

        filter = new AuthorizationGatewayFilter(redisService, keyPair.getPublic(), gatewayProperties);
    }

    @Test
    @DisplayName("Internal path returns 403 directly")
    void filter_internalPath_returnsForbidden() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/internal/member/profile").build());
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.filter(exchange, requestExchange -> {
            invoked.set(true);
            return Mono.empty();
        }).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(invoked).isFalse();
    }

    @Test
    @DisplayName("Whitelisted path skips authentication")
    void filter_whitelistedPath_bypassesAuthentication() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/auth/api/v1/login").build());
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.filter(exchange, requestExchange -> {
            invoked.set(true);
            return Mono.empty();
        }).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(invoked).isTrue();
    }

    @Test
    @DisplayName("Missing token returns 401")
    void filter_missingToken_returnsUnauthorized() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/member/api/v1/profile").build());

        filter.filter(exchange, requestExchange -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Valid token injects user headers and allows through")
    void filter_validToken_injectsHeadersAndContinues() {
        String token = signedToken(5L, null, "digest-v1");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/member/api/v1/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();
        given(redisService.get("auth:perm:version:member:user-1", String.class)).willReturn("5");

        filter.filter(exchange, requestExchange -> {
            forwardedExchange.set(requestExchange);
            return Mono.empty();
        }).block();

        assertThat(forwardedExchange.get()).isNotNull();
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("user-1");
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-User-Domain")).isEqualTo("member");
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-Perm-Version")).isEqualTo("5");
        assertThat(forwardedExchange.get().getRequest().getHeaders().getFirst("X-Perm-Digest")).isEqualTo("digest-v1");
    }

    @Test
    @DisplayName("Expired permission version returns 401")
    void filter_stalePermissionVersion_returnsUnauthorized() {
        String token = signedToken(3L, "member", "digest-v1");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/member/api/v1/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
        given(redisService.get("auth:perm:version:member:user-1", String.class)).willReturn("4");

        filter.filter(exchange, requestExchange -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Downstream exception must not be misclassified as 401")
    void filter_downstreamFailure_propagatesError() {
        String token = signedToken(5L, "member", "digest-v1");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/member/api/v1/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
        given(redisService.get("auth:perm:version:member:user-1", String.class)).willReturn("5");

        GatewayFilterChain chain = requestExchange -> Mono.error(new IllegalStateException("downstream failed"));

        assertThatThrownBy(() -> filter.filter(exchange, chain).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("downstream failed");
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("Blacklisted token returns 401")
    void filter_blacklistedToken_returnsUnauthorized() {
        String jti = "test-jti-blacklisted";
        String token = signedTokenWithId(jti, 5L, "member", "digest-v1");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/member/api/v1/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
        given(redisService.exists("auth:blacklist:" + jti)).willReturn(true);

        filter.filter(exchange, requestExchange -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String signedToken(Long permissionVersion, String userDomain, String permissionDigest) {
        return Jwts.builder()
                .subject("user-1")
                .claim("perm_version", permissionVersion)
                .claim("user_domain", userDomain)
                .claim("perm_digest", permissionDigest)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(keyPair.getPrivate())
                .compact();
    }

    private String signedTokenWithId(String jti, Long permissionVersion, String userDomain, String permissionDigest) {
        return Jwts.builder()
                .id(jti)
                .subject("user-1")
                .claim("perm_version", permissionVersion)
                .claim("user_domain", userDomain)
                .claim("perm_digest", permissionDigest)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(keyPair.getPrivate())
                .compact();
    }
}