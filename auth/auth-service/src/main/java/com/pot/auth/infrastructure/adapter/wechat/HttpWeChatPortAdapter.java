package com.pot.auth.infrastructure.adapter.wechat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pot.auth.domain.port.WeChatPort;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.wechat.entity.WeChatUserInfo;
import com.pot.auth.infrastructure.config.WeChatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * WeChat SNS adapter backed by Spring RestClient.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "auth.wechat.enabled", havingValue = "true")
public class HttpWeChatPortAdapter implements WeChatPort {

    private final WeChatProperties properties;
    private final ObjectMapper objectMapper;

    private final RestClient restClient = RestClient.create();

    @Override
    public WeChatUserInfo getUserInfo(String code, String state) {
        log.info("[WeChat] Exchanging authorization code for user info");

        validateProperties();

        try {
            WeChatTokenResponse tokenResponse = exchangeCodeForToken(code);
            return fetchUserInfo(tokenResponse);

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("[WeChat] Failed to fetch user info: {}", e.getMessage(), e);
            throw new DomainException("WeChat authentication failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        log.info("[WeChat] Refreshing access token");

        validateProperties();

        try {
            String url = UriComponentsBuilder.fromUriString(properties.getRefreshTokenUrl())
                    .queryParam("appid", properties.getAppId())
                    .queryParam("grant_type", "refresh_token")
                    .queryParam("refresh_token", refreshToken)
                    .toUriString();

            String responseBody = restClient.get().uri(url).retrieve().body(String.class);
            JsonNode json = objectMapper.readTree(responseBody);

            checkWeChatError(json);

            String newAccessToken = json.path("access_token").asText(null);
            if (newAccessToken == null || newAccessToken.isBlank()) {
                throw new DomainException("WeChat token refresh response missing access_token");
            }

            log.debug("[WeChat] Access token refreshed");
            return newAccessToken;

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("[WeChat] Failed to refresh token: {}", e.getMessage(), e);
            throw new DomainException("WeChat token refresh failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateAccessToken(String accessToken) {
        // Full validation needs the accessToken/openId pair, which is not cached here.
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        log.debug("[WeChat] validateAccessToken called (simplified: non-null token passes)");
        return true;
    }

    /**
     * Exchanges an authorization code for a token and openId.
     */
    private WeChatTokenResponse exchangeCodeForToken(String code) throws Exception {
        String url = UriComponentsBuilder.fromUriString(properties.getTokenUrl())
                .queryParam("appid", properties.getAppId())
                .queryParam("secret", properties.getAppSecret())
                .queryParam("code", code)
                .queryParam("grant_type", "authorization_code")
                .toUriString();

        String responseBody = restClient.get().uri(url).retrieve().body(String.class);
        JsonNode json = objectMapper.readTree(responseBody);

        checkWeChatError(json);

        String accessToken = json.path("access_token").asText(null);
        String openId = json.path("openid").asText(null);

        if (accessToken == null || openId == null) {
            throw new DomainException("WeChat authorization code is invalid or expired");
        }

        long expiresIn = json.path("expires_in").asLong(7200);
        long expiresAt = System.currentTimeMillis() / 1000 + expiresIn;

        return new WeChatTokenResponse(
                accessToken,
                json.path("refresh_token").asText(null),
                openId,
                expiresAt);
    }

    /**
     * Loads WeChat user details with the granted token.
     */
    private WeChatUserInfo fetchUserInfo(WeChatTokenResponse token) throws Exception {
        String url = UriComponentsBuilder.fromUriString(properties.getUserInfoUrl())
                .queryParam("access_token", token.accessToken())
                .queryParam("openid", token.openId())
                .queryParam("lang", "zh_CN")
                .toUriString();

        String responseBody = restClient.get().uri(url).retrieve().body(String.class);
        JsonNode json = objectMapper.readTree(responseBody);

        checkWeChatError(json);

        return WeChatUserInfo.builder()
                .openId(json.path("openid").asText(token.openId()))
                .unionId(json.path("unionid").asText(null))
                .nickname(json.path("nickname").asText(null))
                .avatar(json.path("headimgurl").asText(null))
                .country(json.path("country").asText(null))
                .province(json.path("province").asText(null))
                .city(json.path("city").asText(null))
                .sex(json.path("sex").asInt(0))
                .accessToken(token.accessToken())
                .refreshToken(token.refreshToken())
                .expiresAt(token.expiresAt())
                .build();
    }

    /**
     * Raises a domain exception when WeChat returns an API error.
     */
    private void checkWeChatError(JsonNode json) {
        int errCode = json.path("errcode").asInt(0);
        if (errCode != 0) {
            String errMsg = json.path("errmsg").asText("unknown error");
            log.error("[WeChat] API error — errcode={}, errmsg={}", errCode, errMsg);
            throw new DomainException(
                    String.format("WeChat API error [%d]: %s", errCode, errMsg));
        }
    }

    /**
     * Ensures the WeChat client configuration is complete.
     */
    private void validateProperties() {
        if (!properties.isConfigured()) {
            log.error("[WeChat] AppID or AppSecret not configured");
            throw new DomainException("WeChat login is not configured, please contact your administrator");
        }
    }

    /**
     * Internal representation of a WeChat token response.
     */
    private record WeChatTokenResponse(
            String accessToken,
            String refreshToken,
            String openId,
            Long expiresAt) {
    }
}
