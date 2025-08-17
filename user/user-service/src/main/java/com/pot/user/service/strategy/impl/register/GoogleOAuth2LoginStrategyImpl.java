package com.pot.user.service.strategy.impl.register;

import com.pot.common.id.IdService;
import com.pot.user.service.entity.User;
import com.pot.user.service.enums.OAuth2Enum;
import com.pot.user.service.service.ThirdPartyConnectionService;
import com.pot.user.service.service.UserService;
import com.pot.user.service.strategy.impl.oAuth2.AbstractOAuth2LoginStrategyImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * @author: Pot
 * @created: 2025/4/6 16:45
 * @description: 谷歌Oauth2登录策略
 */
@Service
@Slf4j
public class GoogleOAuth2LoginStrategyImpl extends AbstractOAuth2LoginStrategyImpl {

    public GoogleOAuth2LoginStrategyImpl(OAuth2ClientProperties oAuth2ClientProperties, RestTemplateBuilder restTemplateBuilder, UserService userService, ThirdPartyConnectionService thirdPartyConnectionService, IdService idService) {
        super(oAuth2ClientProperties, restTemplateBuilder, userService, thirdPartyConnectionService, idService);
    }

    @Override
    protected String extractThirdPartyUserId(Map<String, Object> userInfo) {
        return Optional.ofNullable(userInfo.get("sub"))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("Google user info does not contain sub"));
    }

    @Override
    protected User buildUser(Map<String, Object> userInfo) {
        String name = Optional.ofNullable(userInfo.get("name"))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException("Google user info does not contain name"));
        String email = Optional.ofNullable(userInfo.get("email"))
                .map(Object::toString)
                .orElse(null);
        String picture = Optional.ofNullable(userInfo.get("picture"))
                .map(Object::toString)
                .orElse(null);
        return createBaseBuilder()
                .name(name)
                .nickname(name)
                .email(email)
                .avatar(picture)
                .build();
    }

    @Override
    public OAuth2Enum getType() {
        return OAuth2Enum.GOOGLE;
    }
}
