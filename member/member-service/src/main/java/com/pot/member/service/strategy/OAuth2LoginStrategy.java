package com.pot.member.service.strategy;

import com.pot.member.service.controller.response.Tokens;
import com.pot.member.service.enums.OAuth2Enum;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * @author: Pot
 * @created: 2025/4/4 15:47
 * @description: oauth2登录策略接口
 */
public interface OAuth2LoginStrategy {

    void redirectToOauth2Login(HttpServletResponse httpServletResponse);

    Map<String, Object> getOauth2UserInfo(String code);

    Tokens loginOauth2User(Map<String, Object> userInfo);

    /**
     * 获取第三方登录类型
     *
     * @return 登录类型
     */
    OAuth2Enum getType();
}
