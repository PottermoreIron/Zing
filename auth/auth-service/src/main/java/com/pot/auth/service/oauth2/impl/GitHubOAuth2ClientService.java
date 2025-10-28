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
 * @description: GitHub OAuth2客户端实现
 * <p>
 * API文档: https://docs.github.com/en/developers/apps/building-oauth-apps/authorizing-oauth-apps
 */
@Slf4j
@Service
public class GitHubOAuth2ClientService extends AbstractOAuth2ClientService {

    public GitHubOAuth2ClientService(
            OAuth2ClientProperties oauth2Properties,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        super(oauth2Properties, restTemplate, objectMapper);
    }

    @Override
    public OAuth2Provider getProvider() {
        return OAuth2Provider.GITHUB;
    }

    @Override
    protected OAuth2UserInfo parseUserInfo(String responseBody, String accessToken) {
        try {
            Map<String, Object> userMap = objectMapper.readValue(
                    responseBody,
                    new TypeReference<Map<String, Object>>() {
                    }
            );

            // GitHub用户信息字段映射
            return OAuth2UserInfo.builder()
                    .openId(getStringValue(userMap, "id"))
                    .username(getStringValue(userMap, "login"))
                    .nickname(getStringValue(userMap, "name"))
                    .email(getStringValue(userMap, "email"))
                    .avatarUrl(getStringValue(userMap, "avatar_url"))
                    .profileUrl(getStringValue(userMap, "html_url"))
                    .accessToken(accessToken)
                    .build();

        } catch (Exception e) {
            log.error("解析GitHub用户信息失败: {}", responseBody, e);
            throw new RuntimeException("解析GitHub用户信息失败", e);
        }
    }
}

