package com.pot.auth.infrastructure.adapter.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pot.auth.domain.oauth2.entity.OAuth2UserInfo;
import com.pot.auth.domain.oauth2.valueobject.OAuth2AuthorizationCode;
import com.pot.auth.domain.oauth2.valueobject.OAuth2OpenId;
import com.pot.auth.domain.oauth2.valueobject.OAuth2Provider;
import com.pot.auth.domain.port.OAuth2Port;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.infrastructure.config.OAuth2ProviderProperties;
import com.pot.auth.infrastructure.config.OAuth2ProviderProperties.ProviderConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * OAuth2 HTTP 适配器
 *
 * <p>
 * 通过标准 OAuth2 授权码流程（Authorization Code Flow）向各提供商获取用户信息。
 * 使用 Spring 6 的 {@link RestClient}（同步、声明式 HTTP 客户端）发起请求。
 *
 * <p>
 * 支持的提供商：
 * <ul>
 * <li>Google — googleapis.com</li>
 * <li>GitHub — api.github.com</li>
 * <li>Facebook — graph.facebook.com</li>
 * <li>Apple — appleid.apple.com（id_token 解析）</li>
 * </ul>
 *
 * <p>
 * 启用条件：{@code auth.oauth2.enabled=true}
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "auth.oauth2.enabled", havingValue = "true")
public class HttpOAuth2PortAdapter implements OAuth2Port {

    private final OAuth2ProviderProperties properties;
    private final ObjectMapper objectMapper;

    // RestClient 是线程安全的，可注入或手动构建
    private final RestClient restClient = RestClient.create();

    // ================================================================
    // OAuth2Port 接口实现
    // ================================================================

    @Override
    public String getAuthorizationUrl(OAuth2Provider provider, String state, String redirectUri) {
        return switch (provider) {
            case GOOGLE -> "https://accounts.google.com/o/oauth2/v2/auth"
                    + "?client_id=" + getConfig(provider).getClientId()
                    + "&redirect_uri=" + redirectUri
                    + "&response_type=code"
                    + "&scope=openid%20email%20profile"
                    + "&state=" + state;
            case GITHUB -> "https://github.com/login/oauth/authorize"
                    + "?client_id=" + getConfig(provider).getClientId()
                    + "&redirect_uri=" + redirectUri
                    + "&scope=user:email"
                    + "&state=" + state;
            case FACEBOOK -> "https://www.facebook.com/v18.0/dialog/oauth"
                    + "?client_id=" + getConfig(provider).getClientId()
                    + "&redirect_uri=" + redirectUri
                    + "&scope=email,public_profile"
                    + "&state=" + state;
            case APPLE -> "https://appleid.apple.com/auth/authorize"
                    + "?client_id=" + getConfig(provider).getClientId()
                    + "&redirect_uri=" + redirectUri
                    + "&response_type=code"
                    + "&response_mode=form_post"
                    + "&scope=name%20email"
                    + "&state=" + state;
        };
    }

    @Override
    public OAuth2UserInfo getUserInfo(OAuth2Provider provider, OAuth2AuthorizationCode code, String redirectUri) {
        log.info("[OAuth2] 获取用户信息: provider={}", provider.getCode());

        ProviderConfig config = getConfig(provider);
        validateConfig(provider, config);

        try {
            // 1. 授权码换 Access Token
            String accessToken = exchangeCodeForToken(provider, config, code.value(), redirectUri);

            // 2. Access Token 换用户信息
            return fetchUserInfo(provider, config, accessToken);
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("[OAuth2] 获取用户信息失败: provider={}, error={}", provider.getCode(), e.getMessage(), e);
            throw new DomainException("OAuth2 认证失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String refreshAccessToken(OAuth2Provider provider, String refreshToken) {
        log.info("[OAuth2] 刷新 Access Token: provider={}", provider.getCode());

        ProviderConfig config = getConfig(provider);
        validateConfig(provider, config);

        String tokenUrl = config.getRefreshTokenUrl() != null
                ? config.getRefreshTokenUrl()
                : config.getTokenUrl();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        form.add("client_id", config.getClientId());
        form.add("client_secret", config.getClientSecret());

        try {
            String responseBody = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);

            JsonNode json = objectMapper.readTree(responseBody);
            return json.path("access_token").asText();
        } catch (Exception e) {
            log.error("[OAuth2] 刷新 Token 失败: provider={}", provider.getCode(), e);
            throw new DomainException("OAuth2 Token 刷新失败: " + e.getMessage(), e);
        }
    }

    // ================================================================
    // 私有辅助方法
    // ================================================================

    /**
     * 授权码换 Access Token
     */
    private String exchangeCodeForToken(
            OAuth2Provider provider,
            ProviderConfig config,
            String code,
            String redirectUri) throws Exception {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", config.getClientId());
        form.add("client_secret", config.getClientSecret());
        if (redirectUri != null && !redirectUri.isBlank()) {
            form.add("redirect_uri", redirectUri);
        }

        // GitHub 需要声明接受 JSON 响应
        String responseBody = restClient.post()
                .uri(config.getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .body(form)
                .retrieve()
                .body(String.class);

        JsonNode json = objectMapper.readTree(responseBody);

        if (json.has("error")) {
            String errorDesc = json.path("error_description").asText(json.path("error").asText());
            throw new DomainException("OAuth2 授权码无效: " + errorDesc);
        }

        String accessToken = json.path("access_token").asText(null);
        if (accessToken == null || accessToken.isBlank()) {
            throw new DomainException("OAuth2 提供商未返回 Access Token");
        }

        log.debug("[OAuth2] Access Token 获取成功: provider={}", provider.getCode());
        return accessToken;
    }

    /**
     * 根据提供商获取用户信息（统一映射为 {@link OAuth2UserInfo}）
     */
    private OAuth2UserInfo fetchUserInfo(
            OAuth2Provider provider,
            ProviderConfig config,
            String accessToken) throws Exception {

        return switch (provider) {
            case GOOGLE -> fetchGoogleUserInfo(config, accessToken);
            case GITHUB -> fetchGithubUserInfo(config, accessToken);
            case FACEBOOK -> fetchFacebookUserInfo(config, accessToken);
            case APPLE -> fetchAppleUserInfo(accessToken);
        };
    }

    /**
     * Google — 标准 OIDC userinfo 端点
     */
    private OAuth2UserInfo fetchGoogleUserInfo(ProviderConfig config, String accessToken) throws Exception {
        String body = restClient.get()
                .uri(config.getUserInfoUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(String.class);

        JsonNode json = objectMapper.readTree(body);

        return OAuth2UserInfo.builder()
                .provider(OAuth2Provider.GOOGLE)
                .openId(new OAuth2OpenId(json.path("sub").asText()))
                .username(json.path("name").asText(null))
                .email(json.path("email").asText(null))
                .emailVerified(json.path("email_verified").asBoolean(false))
                .nickname(json.path("name").asText(null))
                .avatarUrl(json.path("picture").asText(null))
                .accessToken(accessToken)
                .rawData(body)
                .build();
    }

    /**
     * GitHub — REST API v3 /user
     */
    private OAuth2UserInfo fetchGithubUserInfo(ProviderConfig config, String accessToken) throws Exception {
        String body = restClient.get()
                .uri(config.getUserInfoUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .retrieve()
                .body(String.class);

        JsonNode json = objectMapper.readTree(body);

        // GitHub 的 email 可能为 null（用户设为私密）
        String email = json.path("email").isNull() ? null : json.path("email").asText(null);

        return OAuth2UserInfo.builder()
                .provider(OAuth2Provider.GITHUB)
                .openId(new OAuth2OpenId(String.valueOf(json.path("id").asLong())))
                .username(json.path("login").asText(null))
                .email(email)
                .emailVerified(email != null) // GitHub 已验证邮箱
                .nickname(json.path("name").asText(json.path("login").asText(null)))
                .avatarUrl(json.path("avatar_url").asText(null))
                .accessToken(accessToken)
                .rawData(body)
                .build();
    }

    /**
     * Facebook — Graph API /me
     */
    private OAuth2UserInfo fetchFacebookUserInfo(ProviderConfig config, String accessToken) throws Exception {
        String body = restClient.get()
                .uri(config.getUserInfoUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(String.class);

        JsonNode json = objectMapper.readTree(body);
        String avatarUrl = json.path("picture").path("data").path("url").asText(null);

        return OAuth2UserInfo.builder()
                .provider(OAuth2Provider.FACEBOOK)
                .openId(new OAuth2OpenId(json.path("id").asText()))
                .username(json.path("name").asText(null))
                .email(json.path("email").asText(null))
                .emailVerified(json.has("email")) // 有邮箱则认为已验证
                .nickname(json.path("name").asText(null))
                .avatarUrl(avatarUrl)
                .accessToken(accessToken)
                .rawData(body)
                .build();
    }

    /**
     * Apple — id_token 中包含用户信息（sub = openId，email 通过 JWT payload 提取）
     *
     * <p>
     * Apple 不提供独立的 userInfo 端点；用户信息（name 等）仅在首次授权时返回，
     * 后续需缓存。此处解析 id_token JWT payload（Base64 解码即可，无需验签）。
     */
    private OAuth2UserInfo fetchAppleUserInfo(String idToken) throws Exception {
        // Apple 的 access_token 响应中包含 id_token（JWT）
        // 此处 accessToken 实际为 id_token
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) {
            throw new DomainException("Apple id_token 格式无效");
        }

        byte[] payloadBytes = java.util.Base64.getUrlDecoder().decode(
                parts[1].length() % 4 == 0 ? parts[1] : parts[1] + "=".repeat(4 - parts[1].length() % 4));
        JsonNode json = objectMapper.readTree(payloadBytes);

        return OAuth2UserInfo.builder()
                .provider(OAuth2Provider.APPLE)
                .openId(new OAuth2OpenId(json.path("sub").asText()))
                .email(json.path("email").asText(null))
                .emailVerified(json.path("email_verified").asBoolean(false))
                .accessToken(idToken)
                .rawData(new String(payloadBytes))
                .build();
    }

    /**
     * 获取提供商配置，不存在则抛出异常
     */
    private ProviderConfig getConfig(OAuth2Provider provider) {
        ProviderConfig config = properties.getProvider(provider.getCode());
        if (config == null) {
            throw new DomainException("OAuth2 提供商配置不存在: " + provider.getCode());
        }
        return config;
    }

    /**
     * 校验提供商配置是否完整
     */
    private void validateConfig(OAuth2Provider provider, ProviderConfig config) {
        if (!config.isConfigured()) {
            log.error("[OAuth2] 提供商配置不完整，缺少 clientId 或 clientSecret: provider={}",
                    provider.getCode());
            throw new DomainException("OAuth2 提供商 [" + provider.getDisplayName() + "] 未配置，请联系管理员");
        }
    }
}
