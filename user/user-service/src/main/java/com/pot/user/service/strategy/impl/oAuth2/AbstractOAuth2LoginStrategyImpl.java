package com.pot.user.service.strategy.impl.oAuth2;

import com.pot.common.enums.ResultCode;
import com.pot.common.utils.IdUtils;
import com.pot.common.utils.PasswordUtils;
import com.pot.common.utils.RandomUtils;
import com.pot.user.service.controller.response.Tokens;
import com.pot.user.service.entity.ThirdPartyConnection;
import com.pot.user.service.entity.User;
import com.pot.user.service.enums.IdBizEnum;
import com.pot.user.service.exception.OAuth2Exception;
import com.pot.user.service.service.ThirdPartyConnectionService;
import com.pot.user.service.service.UserService;
import com.pot.user.service.strategy.OAuth2LoginStrategy;
import com.pot.user.service.utils.CommonUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author: Pot
 * @created: 2025/4/6 15:06
 * @description: 抽象Oauth2实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public abstract class AbstractOAuth2LoginStrategyImpl implements OAuth2LoginStrategy {
    protected final OAuth2ClientProperties oAuth2ClientProperties;
    protected final RestTemplateBuilder restTemplateBuilder;
    protected final UserService userService;
    protected final ThirdPartyConnectionService thirdPartyConnectionService;
    protected final PasswordUtils passwordUtils;
    protected final IdUtils idUtils;

    @Override
    public void redirectToOauth2Login(HttpServletResponse httpServletResponse) {
        String authorizationUrl = buildAuthorizationUrl();
        try {
            // redirect to the authorization URL
            httpServletResponse.sendRedirect(authorizationUrl);
        } catch (Exception e) {
            log.error("Failed to redirect to OAuth2 login: {}", e.getMessage());
            throw new OAuth2Exception(ResultCode.OAUTH2_EXCEPTION, "Failed to redirect to OAuth2 login");
        }
    }

    @Override
    public Map<String, Object> getOauth2UserInfo(String code) {
        // Exchange the authorization code for an access token
        String accessToken = getAccessToken(code);
        return getUserInfo(accessToken);
    }

    @Override
    public Tokens loginOauth2User(Map<String, Object> userInfo) {
        // Extract user information from the userInfo map
        String thirdPartyUserId = extractThirdPartyUserId(userInfo);
        // Check if the user already exists in the system
        User user = userService.findByThirdPartyUserIdAndType(thirdPartyUserId, getType());
        if (user == null) {
            // If the user does not exist, register a new user
            user = registerNewUser(userInfo);
        }
        Long uid = user.getUid();
        // todo update login time and login ip
        return CommonUtils.createAccessTokenAndRefreshToken(uid);
    }

    protected String buildAuthorizationUrl() {
        // Build the authorization URL using the provider's authorization endpoint
        return UriComponentsBuilder.fromUriString(getProviderProperty("authorizationUri"))
                .queryParam("client_id", getRegistrationProperty("clientId"))
                .queryParam("redirect_uri", getRegistrationProperty("redirectUri"))
                .queryParam("scope", getScopes())
                .queryParam("response_type", "code")
                .queryParam("state", generateSecureState())
                .build()
                .toUriString();
    }

    protected String getAccessToken(String code) {
        String tokenUrl = getProviderProperty("tokenUri");
        // Build the request body for the token exchange
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = buildTokenRequestBody(code);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = restTemplateBuilder.build();
        // Send the request to exchange the authorization code for an access token
        try {
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );


            Map<String, Object> response = responseEntity.getBody();
            if (response == null || !response.containsKey("access_token")) {
                throw new OAuth2Exception(ResultCode.OAUTH2_EXCEPTION, "Failed to retrieve access token");
            }

            return response.get("access_token").toString();
        } catch (Exception e) {
            log.error("Error retrieving access token: {}", e.getMessage());
            throw new OAuth2Exception(ResultCode.OAUTH2_EXCEPTION, "Failed to retrieve access token");
        }
    }

    protected Map<String, Object> getUserInfo(String accessToken) {
        String userInfoUrl = getProviderProperty("userInfoUri");
        // Build the request to retrieve user information
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = restTemplateBuilder.build();
        // Send the request to retrieve user information
        try {
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            Map<String, Object> userInfo = responseEntity.getBody();
            if (userInfo == null) {
                throw new OAuth2Exception(ResultCode.OAUTH2_EXCEPTION, "Failed to retrieve user info");
            }
            return userInfo;
        } catch (Exception e) {
            log.error("Error retrieving user info: {}", e.getMessage());
            throw new OAuth2Exception(ResultCode.OAUTH2_EXCEPTION, "Failed to retrieve user info");
        }
    }

    protected MultiValueMap<String, String> buildTokenRequestBody(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", getRegistrationProperty("clientId"));
        body.add("client_secret", getRegistrationProperty("clientSecret"));
        body.add("code", code);
        body.add("redirect_uri", getRegistrationProperty("redirectUri"));
        body.add("grant_type", "authorization_code");
        return body;
    }

    protected User.UserBuilder createBaseBuilder() {
        return User.builder()
                .uid(idUtils.getNextId(IdBizEnum.USER.getBizType()))
                .registerTime(LocalDateTime.now())
                .password(passwordUtils.generateDefaultPassword())
                .status(1)
                .deleted(false);
    }

    protected User registerNewUser(Map<String, Object> userInfo) {
        User user = buildUser(userInfo);
        userService.save(user);
        String thirdPartyUserId = extractThirdPartyUserId(userInfo);
        registerThirdPartyConnection(user, thirdPartyUserId);
        return user;
    }

    protected void registerThirdPartyConnection(User user, String thirdPartyUserId) {
        ThirdPartyConnection connection = ThirdPartyConnection.builder()
                .connectionId(idUtils.getNextId(IdBizEnum.THIRD_PARTY_CONNECTION.getBizType()))
                .platformType(getType().getName())
                .thirdPartyUserId(thirdPartyUserId)
                .uid(user.getUid())
                .build();
        thirdPartyConnectionService.save(connection);
    }

    protected String getProviderTypeKey() {
        return getType().name().toLowerCase();
    }

    protected String getRegistrationProperty(String property) {
        OAuth2ClientProperties.Registration registration = oAuth2ClientProperties.getRegistration()
                .get(getProviderTypeKey());

        return switch (property) {
            case "clientId" -> registration.getClientId();
            case "clientSecret" -> registration.getClientSecret();
            case "redirectUri" -> registration.getRedirectUri();
            case "authorizationGrantType" -> registration.getAuthorizationGrantType();
            default -> throw new IllegalArgumentException(STR."Unknown client property: \{property}");
        };
    }

    protected String getProviderProperty(String property) {
        OAuth2ClientProperties.Provider provider = oAuth2ClientProperties.getProvider()
                .get(getProviderTypeKey());

        return switch (property) {
            case "authorizationUri" -> provider.getAuthorizationUri();
            case "tokenUri" -> provider.getTokenUri();
            case "userInfoUri" -> provider.getUserInfoUri();
            case "userNameAttribute" -> provider.getUserNameAttribute();
            default -> throw new IllegalArgumentException("Unknown provider property: " + property);
        };
    }

    protected String generateSecureState() {
        return RandomUtils.generateRandomString(32);
    }

    protected String getScopes() {
        Set<String> scopes = oAuth2ClientProperties.getRegistration()
                .get(getProviderTypeKey())
                .getScope();

        return String.join(" ", scopes);
    }

    protected abstract String extractThirdPartyUserId(Map<String, Object> userInfo);

    protected abstract User buildUser(Map<String, Object> userInfo);
}
