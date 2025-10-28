package com.pot.auth.service.oauth2.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pot.auth.service.config.OAuth2ClientProperties;
import com.pot.auth.service.dto.oauth2.OAuth2UserInfo;
import com.pot.auth.service.enums.OAuth2Provider;
import com.pot.auth.service.oauth2.AbstractOAuth2ClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * @author: Pot
 * @created: 2025/10/22
 * @description: Google OAuth2客户端实现
 * <p>
 * API文档: https://developers.google.com/identity/protocols/oauth2
 */
@Slf4j
@Service
public class GoogleOAuth2ClientService extends AbstractOAuth2ClientService {

    public GoogleOAuth2ClientService(
            OAuth2ClientProperties oauth2Properties,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        super(oauth2Properties, restTemplate, objectMapper);
    }

    @Override
    public OAuth2Provider getProvider() {
        return OAuth2Provider.GOOGLE;
    }

    @Override
    protected OAuth2UserInfo parseUserInfo(String responseBody, String accessToken) {
        try {
            Map<String, Object> userMap = objectMapper.readValue(
                    responseBody,
                    new TypeReference<Map<String, Object>>() {
                    }
            );

            // Google用户信息字段映射
            return OAuth2UserInfo.builder()
                    .openId(getStringValue(userMap, "sub"))  // Google使用'sub'作为用户ID
                    .username(getStringValue(userMap, "email"))
                    .nickname(getStringValue(userMap, "name"))
                    .email(getStringValue(userMap, "email"))
                    .avatarUrl(getStringValue(userMap, "picture"))
                    .profileUrl(getStringValue(userMap, "profile"))
                    .accessToken(accessToken)
                    .build();

        } catch (Exception e) {
            log.error("解析Google用户信息失败: {}", responseBody, e);
            throw new RuntimeException("解析Google用户信息失败", e);
        }
    }
}

