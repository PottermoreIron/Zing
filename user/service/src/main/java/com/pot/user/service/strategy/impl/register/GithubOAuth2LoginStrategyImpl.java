package com.pot.user.service.strategy.impl.register;

import com.pot.user.service.entity.User;
import com.pot.user.service.enums.OAuth2Enum;
import com.pot.user.service.service.ThirdPartyConnectionService;
import com.pot.user.service.service.UserService;
import com.pot.user.service.strategy.impl.oAuth2.AbstractOAuth2LoginStrategyImpl;
import com.pot.user.service.utils.IdUtils;
import com.pot.user.service.utils.PasswordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * @author: Pot
 * @created: 2025/4/6 16:33
 * @description: Github Oauth2登录策略
 */
@Service
@Slf4j
public class GithubOAuth2LoginStrategyImpl extends AbstractOAuth2LoginStrategyImpl {

    public GithubOAuth2LoginStrategyImpl(OAuth2ClientProperties oAuth2ClientProperties, RestTemplateBuilder restTemplateBuilder, UserService userService, ThirdPartyConnectionService thirdPartyConnectionService, PasswordUtils passwordUtils, IdUtils idUtils) {
        super(oAuth2ClientProperties, restTemplateBuilder, userService, thirdPartyConnectionService, passwordUtils, idUtils);
    }

    @Override
    protected String extractThirdPartyUserId(Map<String, Object> userInfo) {
        return Optional.ofNullable(userInfo.get("node_id"))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("Github user info does not contain node_id"));
    }

    @Override
    protected User buildUser(Map<String, Object> userInfo) {
        String name = Optional.ofNullable(userInfo.get("login"))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("Github user info does not contain name"));
        String email = Optional.ofNullable(userInfo.get("email"))
                .map(Object::toString)
                .orElse(null);
        String avatarUrl = Optional.ofNullable(userInfo.get("avatar_url"))
                .map(Object::toString)
                .orElse(null);
        return createBaseBuilder()
                .name(name)
                .nickname(name)
                .email(email)
                .avatar(avatarUrl)
                .build();
    }

    @Override
    public OAuth2Enum getType() {
        return OAuth2Enum.GITHUB;
    }
}
