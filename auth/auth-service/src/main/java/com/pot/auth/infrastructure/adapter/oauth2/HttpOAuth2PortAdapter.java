package com.pot.auth.infrastructure.adapter.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pot.auth.domain.oauth2.entity.OAuth2UserInfo;
import com.pot.auth.domain.oauth2.valueobject.OAuth2AuthorizationCode;
import com.pot.auth.domain.oauth2.valueobject.OAuth2OpenId;
import com.pot.auth.domain.oauth2.valueobject.OAuth2Provider;
import com.pot.auth.domain.port.OAuth2Port;
import com.pot.auth.domain.shared.enums.AuthResultCode;
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
import org.springframework.web.util.UriComponentsBuilder;

/**
 * OAuth2 adapter backed by Spring RestClient.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "auth.oauth2.enabled", havingValue = "true")
public class HttpOAuth2PortAdapter implements OAuth2Port {

    // Standard OAuth2 parameter names (RFC 6749).
    private static final String PARAM_GRANT_TYPE = "grant_type";
    private static final String PARAM_CODE = "code";
    private static final String PARAM_CLIENT_ID = "client_id";
    private static final String PARAM_CLIENT_SECRET = "client_secret";
    private static final String PARAM_REDIRECT_URI = "redirect_uri";
    private static final String PARAM_RESPONSE_TYPE = "response_type";
    private static final String PARAM_SCOPE = "scope";
    private static final String PARAM_STATE = "state";
    private static final String PARAM_REFRESH_TOKEN = "refresh_token";
    private static final String GRANT_AUTHORIZATION_CODE = "authorization_code";
    private static final String RESPONSE_TYPE_CODE = "code";

    // Standard OAuth2 JSON response field names.
    private static final String FIELD_ACCESS_TOKEN = "access_token";
    private static final String FIELD_ERROR = "error";
    private static final String FIELD_ERROR_DESCRIPTION = "error_description";

    // Common OIDC / OAuth2 userinfo field names shared across providers.
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_EMAIL_VERIFIED = "email_verified";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_SUB = "sub";
    private static final String FIELD_ID = "id";

    // HTTP header values.
    private static final String BEARER_PREFIX = "Bearer ";

    // GitHub API version header.
    private static final String GITHUB_API_VERSION_HEADER = "X-GitHub-Api-Version";
    private static final String GITHUB_API_VERSION = "2022-11-28";
    private static final String GITHUB_ACCEPT_HEADER = "application/vnd.github+json";

    private final OAuth2ProviderProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    @Override
    public String getAuthorizationUrl(OAuth2Provider provider, String state, String redirectUri) {
        ProviderConfig config = getConfig(provider);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(config.getAuthorizationUrl())
                .queryParam(PARAM_CLIENT_ID, config.getClientId())
                .queryParam(PARAM_REDIRECT_URI, redirectUri)
                .queryParam(PARAM_RESPONSE_TYPE, RESPONSE_TYPE_CODE)
                .queryParam(PARAM_SCOPE, config.getScope())
                .queryParam(PARAM_STATE, state);
        config.getExtraAuthParams().forEach(builder::queryParam);
        return builder.toUriString();
    }

    @Override
    public OAuth2UserInfo getUserInfo(OAuth2Provider provider, OAuth2AuthorizationCode code, String redirectUri) {
        log.info("[OAuth2] Fetching user info — provider={}", provider.getCode());

        ProviderConfig config = getConfig(provider);
        validateConfig(provider, config);

        try {
            String accessToken = exchangeCodeForToken(provider, config, code.value(), redirectUri);
            return fetchUserInfo(provider, config, accessToken);
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("[OAuth2] Failed to fetch user info — provider={}, error={}", provider.getCode(), e.getMessage(),
                    e);
            throw new DomainException(AuthResultCode.OAUTH2_CODE_INVALID, e);
        }
    }

    @Override
    public String refreshAccessToken(OAuth2Provider provider, String refreshToken) {
        log.info("[OAuth2] Refreshing access token — provider={}", provider.getCode());

        ProviderConfig config = getConfig(provider);
        validateConfig(provider, config);

        String tokenUrl = config.getRefreshTokenUrl() != null
                ? config.getRefreshTokenUrl()
                : config.getTokenUrl();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(PARAM_GRANT_TYPE, PARAM_REFRESH_TOKEN);
        form.add(PARAM_REFRESH_TOKEN, refreshToken);
        form.add(PARAM_CLIENT_ID, config.getClientId());
        form.add(PARAM_CLIENT_SECRET, config.getClientSecret());

        try {
            String responseBody = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);

            JsonNode json = objectMapper.readTree(responseBody);
            return json.path(FIELD_ACCESS_TOKEN).asText();
        } catch (Exception e) {
            log.error("[OAuth2] Failed to refresh token — provider={}", provider.getCode(), e);
            throw new DomainException(AuthResultCode.OAUTH2_REFRESH_FAILED, e);
        }
    }

    /**
     * Exchanges an authorization code for an access token.
     */
    private String exchangeCodeForToken(
            OAuth2Provider provider,
            ProviderConfig config,
            String code,
            String redirectUri) throws Exception {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(PARAM_GRANT_TYPE, GRANT_AUTHORIZATION_CODE);
        form.add(PARAM_CODE, code);
        form.add(PARAM_CLIENT_ID, config.getClientId());
        form.add(PARAM_CLIENT_SECRET, config.getClientSecret());
        if (redirectUri != null && !redirectUri.isBlank()) {
            form.add(PARAM_REDIRECT_URI, redirectUri);
        }

        // GitHub returns form data by default unless JSON is explicitly requested.
        String responseBody = restClient.post()
                .uri(config.getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .body(form)
                .retrieve()
                .body(String.class);

        JsonNode json = objectMapper.readTree(responseBody);

        if (json.has(FIELD_ERROR)) {
            log.warn("[OAuth2] Token exchange failed — provider={}, error={}", provider.getCode(),
                    json.path(FIELD_ERROR_DESCRIPTION).asText(json.path(FIELD_ERROR).asText()));
            throw new DomainException(AuthResultCode.OAUTH2_CODE_INVALID);
        }

        String accessToken = json.path(FIELD_ACCESS_TOKEN).asText(null);
        if (accessToken == null || accessToken.isBlank()) {
            throw new DomainException(AuthResultCode.OAUTH2_TOKEN_MISSING);
        }

        log.debug("[OAuth2] Access token acquired — provider={}", provider.getCode());
        return accessToken;
    }

    /**
     * Loads user information and maps it to the common OAuth2 model.
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
     * Loads user information from the Google OIDC userinfo endpoint.
     */
    private OAuth2UserInfo fetchGoogleUserInfo(ProviderConfig config, String accessToken) throws Exception {
        String body = restClient.get()
                .uri(config.getUserInfoUrl())
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
                .retrieve()
                .body(String.class);

        JsonNode json = objectMapper.readTree(body);

        return OAuth2UserInfo.builder()
                .provider(OAuth2Provider.GOOGLE)
                .openId(new OAuth2OpenId(json.path(FIELD_SUB).asText()))
                .email(json.path(FIELD_EMAIL).asText(null))
                .emailVerified(json.path(FIELD_EMAIL_VERIFIED).asBoolean(false))
                .nickname(json.path(FIELD_NAME).asText(null))
                .avatarUrl(json.path("picture").asText(null))
                .accessToken(accessToken)
                .rawData(body)
                .build();
    }

    /**
     * Loads user information from the GitHub user API.
     */
    private OAuth2UserInfo fetchGithubUserInfo(ProviderConfig config, String accessToken) throws Exception {
        String body = restClient.get()
                .uri(config.getUserInfoUrl())
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
                .header(HttpHeaders.ACCEPT, GITHUB_ACCEPT_HEADER)
                .header(GITHUB_API_VERSION_HEADER, GITHUB_API_VERSION)
                .retrieve()
                .body(String.class);

        JsonNode json = objectMapper.readTree(body);

        // GitHub may omit the primary email when the user keeps it private.
        String email = json.path(FIELD_EMAIL).isNull() ? null : json.path(FIELD_EMAIL).asText(null);

        return OAuth2UserInfo.builder()
                .provider(OAuth2Provider.GITHUB)
                .openId(new OAuth2OpenId(String.valueOf(json.path(FIELD_ID).asLong())))
                .email(email)
                .emailVerified(email != null)
                .nickname(json.path(FIELD_NAME).asText(json.path("login").asText(null)))
                .avatarUrl(json.path("avatar_url").asText(null))
                .accessToken(accessToken)
                .rawData(body)
                .build();
    }

    /**
     * Loads user information from the Facebook Graph API.
     */
    private OAuth2UserInfo fetchFacebookUserInfo(ProviderConfig config, String accessToken) throws Exception {
        String body = restClient.get()
                .uri(config.getUserInfoUrl())
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + accessToken)
                .retrieve()
                .body(String.class);

        JsonNode json = objectMapper.readTree(body);
        String avatarUrl = json.path("picture").path("data").path("url").asText(null);

        return OAuth2UserInfo.builder()
                .provider(OAuth2Provider.FACEBOOK)
                .openId(new OAuth2OpenId(json.path(FIELD_ID).asText()))
                .email(json.path(FIELD_EMAIL).asText(null))
                .emailVerified(json.has(FIELD_EMAIL))
                .nickname(json.path(FIELD_NAME).asText(null))
                .avatarUrl(avatarUrl)
                .accessToken(accessToken)
                .rawData(body)
                .build();
    }

    /**
     * Loads Apple user information from the id_token payload.
     */
    private OAuth2UserInfo fetchAppleUserInfo(String idToken) throws Exception {
        // Apple user details are carried in the id_token payload rather than a userinfo
        // endpoint.
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) {
            throw new DomainException(AuthResultCode.OAUTH2_TOKEN_INVALID);
        }

        byte[] payloadBytes = java.util.Base64.getUrlDecoder().decode(
                parts[1].length() % 4 == 0 ? parts[1] : parts[1] + "=".repeat(4 - parts[1].length() % 4));
        JsonNode json = objectMapper.readTree(payloadBytes);

        return OAuth2UserInfo.builder()
                .provider(OAuth2Provider.APPLE)
                .openId(new OAuth2OpenId(json.path(FIELD_SUB).asText()))
                .email(json.path(FIELD_EMAIL).asText(null))
                .emailVerified(json.path(FIELD_EMAIL_VERIFIED).asBoolean(false))
                .accessToken(idToken)
                .rawData(new String(payloadBytes))
                .build();
    }

    /**
     * Returns the configured provider settings.
     */
    private ProviderConfig getConfig(OAuth2Provider provider) {
        ProviderConfig config = properties.getProvider(provider.getCode());
        if (config == null) {
            throw new DomainException(AuthResultCode.OAUTH2_NOT_CONFIGURED);
        }
        return config;
    }

    /**
     * Ensures the provider configuration is complete.
     */
    private void validateConfig(OAuth2Provider provider, ProviderConfig config) {
        if (!config.isConfigured()) {
            log.error("[OAuth2] Provider configuration incomplete, missing clientId or clientSecret — provider={}",
                    provider.getCode());
            throw new DomainException(AuthResultCode.OAUTH2_NOT_CONFIGURED);
        }
    }
}
