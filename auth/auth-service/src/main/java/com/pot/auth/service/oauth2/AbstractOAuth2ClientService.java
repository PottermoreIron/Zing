package com.pot.auth.service.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pot.auth.service.config.OAuth2ClientProperties;
import com.pot.auth.service.dto.oauth2.OAuth2TokenResponse;
import com.pot.auth.service.dto.oauth2.OAuth2UserInfo;
import com.pot.zing.framework.common.excption.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * @author: Pot
 * @created: 2025/10/22
 * @description: OAuth2客户端服务抽象基类 - 实现OAuth2标准流程的模板方法
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractOAuth2ClientService implements OAuth2ClientService {

    protected final OAuth2ClientProperties oauth2Properties;
    protected final RestTemplate restTemplate;
    protected final ObjectMapper objectMapper;

    /**
     * 获取当前提供商的配置
     */
    protected OAuth2ClientProperties.OAuth2ClientConfig getClientConfig() {
        String providerKey = getProvider().getProvider();
        OAuth2ClientProperties.OAuth2ClientConfig config = oauth2Properties.getClients().get(providerKey);

        if (config == null || !config.getEnabled()) {
            throw new BusinessException(getProvider().getDisplayName() + " OAuth2登录未配置或未启用");
        }

        return config;
    }

    @Override
    public String getAuthorizationUrl(String state) {
        OAuth2ClientProperties.OAuth2ClientConfig config = getClientConfig();

        return UriComponentsBuilder
                .fromHttpUrl(config.getAuthorizationUri())
                .queryParam("client_id", config.getClientId())
                .queryParam("redirect_uri", config.getRedirectUri())
                .queryParam("scope", config.getScope())
                .queryParam("state", state)
                .queryParam("response_type", "code")
                .build()
                .toUriString();
    }

    @Override
    public OAuth2TokenResponse exchangeToken(String code) {
        OAuth2ClientProperties.OAuth2ClientConfig config = getClientConfig();

        log.info("开始使用授权码换取Token: provider={}", getProvider().getProvider());

        // 构建请求参数
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("client_id", config.getClientId());
        params.add("client_secret", config.getClientSecret());
        params.add("redirect_uri", config.getRedirectUri());

        // 添加提供商特定的参数
        addExtraTokenParams(params);

        // 发送请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    config.getTokenUri(),
                    request,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new BusinessException("获取OAuth2 Token失败");
            }

            return parseTokenResponse(response.getBody());

        } catch (Exception e) {
            log.error("OAuth2 Token交换失败: provider={}", getProvider().getProvider(), e);
            throw new BusinessException("OAuth2登录失败: " + e.getMessage());
        }
    }

    @Override
    public OAuth2UserInfo getUserInfo(String accessToken) {
        OAuth2ClientProperties.OAuth2ClientConfig config = getClientConfig();

        log.info("开始获取OAuth2用户信息: provider={}", getProvider().getProvider());

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getUserInfoUri(),
                    HttpMethod.GET,
                    request,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new BusinessException("获取OAuth2用户信息失败");
            }

            // 解析用户信息
            OAuth2UserInfo userInfo = parseUserInfo(response.getBody(), accessToken);
            userInfo.setProvider(getProvider().getProvider());
            userInfo.setRawData(response.getBody());

            log.info("OAuth2用户信息获取成功: provider={}, openId={}",
                    getProvider().getProvider(), userInfo.getOpenId());

            return userInfo;

        } catch (Exception e) {
            log.error("获取OAuth2用户信息失败: provider={}", getProvider().getProvider(), e);
            throw new BusinessException("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 添加提供商特定的Token请求参数（子类可覆盖）
     */
    protected void addExtraTokenParams(MultiValueMap<String, String> params) {
        // 默认不添加额外参数
    }

    /**
     * 解析Token响应（子类可覆盖以支持不同的响应格式）
     */
    protected OAuth2TokenResponse parseTokenResponse(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, OAuth2TokenResponse.class);
        } catch (JsonProcessingException e) {
            log.error("解析Token响应失败: {}", responseBody, e);
            throw new BusinessException("解析OAuth2 Token响应失败");
        }
    }

    /**
     * 解析用户信息（子类必须实现，因为每个提供商的用户信息格式不同）
     */
    protected abstract OAuth2UserInfo parseUserInfo(String responseBody, String accessToken);

    /**
     * 从JSON中安全获取字符串值
     */
    @SuppressWarnings("unchecked")
    protected String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}

