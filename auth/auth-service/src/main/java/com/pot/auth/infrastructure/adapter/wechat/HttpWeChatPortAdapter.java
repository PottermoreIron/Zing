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
 * 微信 OAuth2 适配器
 *
 * <p>
 * 通过微信官方 SNS API 实现授权码换 access_token、获取用户信息等功能，
 * 无需引入第三方 SDK，使用 Spring 6 {@link RestClient} 直接发起 HTTP 请求。
 *
 * <p>
 * 接口文档参考：
 * <ul>
 * <li>网页授权 Access
 * Token：{@code GET https://api.weixin.qq.com/sns/oauth2/access_token}</li>
 * <li>刷新 Access
 * Token：{@code GET https://api.weixin.qq.com/sns/oauth2/refresh_token}</li>
 * <li>获取用户基本信息：{@code GET https://api.weixin.qq.com/sns/userinfo}</li>
 * <li>检验 Access Token：{@code GET https://api.weixin.qq.com/sns/auth}</li>
 * </ul>
 *
 * <p>
 * 启用条件：{@code auth.wechat.enabled=true}
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "auth.wechat.enabled", havingValue = "true")
public class HttpWeChatPortAdapter implements WeChatPort {

    private final WeChatProperties properties;
    private final ObjectMapper objectMapper;

    private final RestClient restClient = RestClient.create();

    // ================================================================
    // WeChatPort 接口实现
    // ================================================================

    @Override
    public WeChatUserInfo getUserInfo(String code, String state) {
        log.info("[微信] 授权码换取用户信息");

        validateProperties();

        try {
            // Step 1：授权码换 access_token（同时返回 openId）
            WeChatTokenResponse tokenResponse = exchangeCodeForToken(code);

            // Step 2：access_token + openId 换用户详细信息
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
        // 微信需要同时传 access_token 和 openid 才能验证，此处只做基础验证
        // 如需完整验证，需要缓存 openId 与 accessToken 的映射
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        log.debug("[微信] validateAccessToken 调用（简化实现，Token 非空即通过）");
        return true;
    }

    // ================================================================
    // 私有辅助方法
    // ================================================================

    /**
     * 授权码换 Access Token + OpenID
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
     * 使用 Access Token + OpenID 获取用户详细信息
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
     * 检查微信 API 错误响应
     *
     * <p>
     * 微信接口错误时返回 {@code {"errcode": 40029, "errmsg": "invalid code"}}
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
     * 校验微信配置是否完整
     */
    private void validateProperties() {
        if (!properties.isConfigured()) {
            log.error("[微信] AppID 或 AppSecret 未配置");
            throw new DomainException("微信登录功能未配置，请联系管理员");
        }
    }

    /**
     * 微信 Token 响应的内部数据传输对象
     */
    private record WeChatTokenResponse(
            String accessToken,
            String refreshToken,
            String openId,
            Long expiresAt) {
    }
}
