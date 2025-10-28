package com.pot.auth.service.oauth2.wechat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pot.auth.service.config.OAuth2ClientProperties;
import com.pot.auth.service.dto.oauth2.OAuth2TokenResponse;
import com.pot.auth.service.dto.oauth2.OAuth2UserInfo;
import com.pot.auth.service.enums.OAuth2Provider;
import com.pot.auth.service.oauth2.AbstractOAuth2ClientService;
import com.pot.zing.framework.common.excption.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * @author: Pot
 * @created: 2025/10/23
 * @description: 微信OAuth2客户端服务实现
 * <p>
 * 直接使用HTTP调用微信API，保持与现有OAuth2架构的一致性
 * API文档: https://developers.weixin.qq.com/doc/oplatform/Website_App/WeChat_Login/Wechat_Login.html
 */
@Slf4j
@Service
public class WeChatOAuth2ClientService extends AbstractOAuth2ClientService {

    public WeChatOAuth2ClientService(
            OAuth2ClientProperties oauth2Properties,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        super(oauth2Properties, restTemplate, objectMapper);
    }

    @Override
    public OAuth2Provider getProvider() {
        return OAuth2Provider.WECHAT;
    }

    @Override
    public String getAuthorizationUrl(String state) {
        OAuth2ClientProperties.OAuth2ClientConfig config = getClientConfig();

        return UriComponentsBuilder
                .fromHttpUrl(config.getAuthorizationUri())
                .queryParam("appid", config.getClientId())
                .queryParam("redirect_uri", config.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", config.getScope())
                .queryParam("state", state)
                .fragment("wechat_redirect")
                .build()
                .toUriString();
    }

    @Override
    public OAuth2TokenResponse exchangeToken(String code) {
        OAuth2ClientProperties.OAuth2ClientConfig config = getClientConfig();
        log.info("开始使用授权码换取微信Token: code={}", code);

        try {
            String tokenUrl = UriComponentsBuilder
                    .fromHttpUrl(config.getTokenUri())
                    .queryParam("appid", config.getClientId())
                    .queryParam("secret", config.getClientSecret())
                    .queryParam("code", code)
                    .queryParam("grant_type", "authorization_code")
                    .build()
                    .toUriString();

            String responseBody = restTemplate.getForObject(tokenUrl, String.class);

            if (responseBody == null) {
                throw new BusinessException("微信Token响应为空");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            if (responseMap.containsKey("errcode")) {
                Integer errcode = (Integer) responseMap.get("errcode");
                String errmsg = (String) responseMap.get("errmsg");
                throw new BusinessException("微信Token获取失败: " + errcode + " - " + errmsg);
            }

            OAuth2TokenResponse tokenResponse = OAuth2TokenResponse.builder()
                    .accessToken(getStringValue(responseMap, "access_token"))
                    .refreshToken(getStringValue(responseMap, "refresh_token"))
                    .expiresIn(getLongValue(responseMap, "expires_in"))
                    .openId(getStringValue(responseMap, "openid"))
                    .unionId(getStringValue(responseMap, "unionid"))
                    .scope(getStringValue(responseMap, "scope"))
                    .build();

            log.info("微信Token获取成功: openId={}, unionId={}",
                    tokenResponse.getOpenId(), tokenResponse.getUnionId());

            return tokenResponse;

        } catch (Exception e) {
            log.error("微信Token交换失败", e);
            throw new BusinessException("微信登录失败: " + e.getMessage());
        }
    }

    @Override
    public OAuth2UserInfo getUserInfo(String accessToken) {
        log.info("开始获取微信用户信息");

        try {
            OAuth2TokenResponse tokenResponse = extractTokenResponse(accessToken);

            if (tokenResponse == null || tokenResponse.getOpenId() == null) {
                throw new BusinessException("无法获取微信openId");
            }

            OAuth2ClientProperties.OAuth2ClientConfig config = getClientConfig();
            String userInfoUrl = UriComponentsBuilder
                    .fromHttpUrl(config.getUserInfoUri())
                    .queryParam("access_token", tokenResponse.getAccessToken())
                    .queryParam("openid", tokenResponse.getOpenId())
                    .queryParam("lang", "zh_CN")
                    .build()
                    .toUriString();

            String responseBody = restTemplate.getForObject(userInfoUrl, String.class);

            if (responseBody == null) {
                throw new BusinessException("微信用户信息响应为空");
            }

            OAuth2UserInfo userInfo = parseUserInfo(responseBody, tokenResponse.getAccessToken());
            userInfo.setOpenId(tokenResponse.getOpenId());
            userInfo.setUnionId(tokenResponse.getUnionId());
            userInfo.setProvider(getProvider().getProvider());
            userInfo.setRawData(responseBody);

            log.info("微信用户信息获取成功: openId={}, nickname={}",
                    userInfo.getOpenId(), userInfo.getNickname());

            return userInfo;

        } catch (Exception e) {
            log.error("获取微信用户信息失败", e);
            throw new BusinessException("获取微信用户信息失败: " + e.getMessage());
        }
    }

    @Override
    protected OAuth2UserInfo parseUserInfo(String responseBody, String accessToken) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = objectMapper.readValue(responseBody, Map.class);

            if (userMap.containsKey("errcode")) {
                Integer errcode = (Integer) userMap.get("errcode");
                String errmsg = (String) userMap.get("errmsg");
                throw new BusinessException("微信用户信息获取失败: " + errcode + " - " + errmsg);
            }

            return OAuth2UserInfo.builder()
                    .openId(getStringValue(userMap, "openid"))
                    .unionId(getStringValue(userMap, "unionid"))
                    .nickname(getStringValue(userMap, "nickname"))
                    .avatarUrl(getStringValue(userMap, "headimgurl"))
                    .gender(getIntegerValue(userMap, "sex"))
                    .country(getStringValue(userMap, "country"))
                    .province(getStringValue(userMap, "province"))
                    .city(getStringValue(userMap, "city"))
                    .language(getStringValue(userMap, "language"))
                    .accessToken(accessToken)
                    .build();

        } catch (Exception e) {
            log.error("解析微信用户信息失败: {}", responseBody, e);
            throw new BusinessException("解析微信用户信息失败: " + e.getMessage());
        }
    }

    private OAuth2TokenResponse extractTokenResponse(String accessToken) {
        try {
            if (accessToken.startsWith("{")) {
                return objectMapper.readValue(accessToken, OAuth2TokenResponse.class);
            }
            return OAuth2TokenResponse.builder()
                    .accessToken(accessToken)
                    .build();
        } catch (Exception e) {
            log.warn("无法解析TokenResponse，使用accessToken作为降级方案", e);
            return OAuth2TokenResponse.builder()
                    .accessToken(accessToken)
                    .build();
        }
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

