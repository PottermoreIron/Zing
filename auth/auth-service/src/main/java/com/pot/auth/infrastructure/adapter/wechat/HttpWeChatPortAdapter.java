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
        log.info("[微信] 授权码换取用户信息");

        validateProperties();

        try {
            WeChatTokenResponse tokenResponse = exchangeCodeForToken(code);
            return fetchUserInfo(tokenResponse);

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("[微信] 获取用户信息失败: {}", e.getMessage(), e);
            throw new DomainException("微信认证失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        log.info("[微信] 刷新 Access Token");

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
                throw new DomainException("微信刷新 Token 响应缺少 access_token");
            }

            log.debug("[微信] Access Token 刷新成功");
            return newAccessToken;

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("[微信] 刷新 Token 失败: {}", e.getMessage(), e);
            throw new DomainException("微信 Token 刷新失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateAccessToken(String accessToken) {
        // Full validation needs the accessToken/openId pair, which is not cached here.
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        log.debug("[微信] validateAccessToken 调用（简化实现，Token 非空即通过）");
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
            throw new DomainException("微信授权码无效或已过期");
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
            String errMsg = json.path("errmsg").asText("未知错误");
            log.error("[微信] API 返回错误: errcode={}, errmsg={}", errCode, errMsg);
            throw new DomainException(
                    String.format("微信 API 错误 [%d]: %s", errCode, errMsg));
        }
    }

    /**
     * Ensures the WeChat client configuration is complete.
     */
    private void validateProperties() {
        if (!properties.isConfigured()) {
            log.error("[微信] AppID 或 AppSecret 未配置");
            throw new DomainException("微信登录功能未配置，请联系管理员");
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
