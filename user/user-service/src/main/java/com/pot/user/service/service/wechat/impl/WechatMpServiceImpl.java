package com.pot.user.service.service.wechat.impl;

import com.pot.common.id.IdService;
import com.pot.common.utils.PasswordUtils;
import com.pot.user.service.entity.ThirdPartyConnection;
import com.pot.user.service.entity.User;
import com.pot.user.service.enums.IdBizEnum;
import com.pot.user.service.enums.OAuth2Enum;
import com.pot.user.service.handler.wechat.builder.TextWechatMpBuilder;
import com.pot.user.service.service.ThirdPartyConnectionService;
import com.pot.user.service.service.UserService;
import com.pot.user.service.service.wechat.WechatMpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * @author: Pot
 * @created: 2025/4/12 21:43
 * @description: 基于微信公众号的扫码登录
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WechatMpServiceImpl implements WechatMpService {

    private static final String OAUTH_URL_TEMPLATE =
            "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";

    @Value("${wechat.mp.callbackUrl}")
    private String callbackUrl;

    private final UserService userService;
    private final ThirdPartyConnectionService thirdPartyConnectionService;
    private final IdService idService;

    @Override
    public WxMpXmlOutMessage scan(WxMpService wxMpService, WxMpXmlMessage wxMpXmlMessage) {
        String openId = wxMpXmlMessage.getFromUser();
        log.debug("Processing scan request for openid: {}", openId);

        return findUserByOpenId(openId)
                .map(user -> createWelcomeBackMessage(user, wxMpXmlMessage, wxMpService))
                .orElseGet(() -> initiateAuthFlow(openId, wxMpService, wxMpXmlMessage));
    }

    @Override
    public void authorize(WxOAuth2UserInfo userInfo) {
        String openId = userInfo.getOpenid();
        log.info("Processing authorization for user: {}", openId);

        findUserByOpenId(openId)
                .ifPresentOrElse(
                        user -> updateUserInfo(user, userInfo),
                        () -> createNewUser(openId, userInfo)
                );
    }

    // Private methods with clear responsibilities

    private Optional<User> findUserByOpenId(String openId) {
        ThirdPartyConnection connection = thirdPartyConnectionService.lambdaQuery()
                .eq(ThirdPartyConnection::getThirdPartyUserId, openId)
                .eq(ThirdPartyConnection::getPlatformType, OAuth2Enum.WECHAT.getName())
                .eq(ThirdPartyConnection::getDeleted, false)
                .one();

        if (connection == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(userService.lambdaQuery()
                .eq(User::getUid, connection.getUid())
                .one());
    }

    private WxMpXmlOutMessage createWelcomeBackMessage(User user, WxMpXmlMessage wxMpXmlMessage, WxMpService wxMpService) {
        log.info("Existing user logged in: {}", user.getId());
        return buildTextMessage(
                STR."欢迎回来，\{user.getNickname()}！",
                wxMpXmlMessage,
                wxMpService
        );
    }

    private WxMpXmlOutMessage initiateAuthFlow(String openId, WxMpService wxMpService, WxMpXmlMessage wxMpXmlMessage) {
        log.info("New user scan detected, initiating authorization flow for: {}", openId);

        try {
            String authUrl = createAuthorizationUrl(wxMpService.getWxMpConfigStorage().getAppId());
            return buildTextMessage(
                    STR."Hello! 请点击<a href=\"\{authUrl}\">这里</a>进行授权",
                    wxMpXmlMessage,
                    wxMpService
            );
        } catch (Exception e) {
            log.error("Failed to create authorization message", e);
            return buildTextMessage(
                    "抱歉，遇到了点问题，请稍后再试。",
                    wxMpXmlMessage,
                    wxMpService
            );
        }
    }

    private String createAuthorizationUrl(String appId) {
        String redirect = STR."\{callbackUrl}/api/wechat/\{appId}/get/weChatUserInfo";
        return String.format(OAUTH_URL_TEMPLATE, appId, URLEncoder.encode(redirect, StandardCharsets.UTF_8));
    }

    private void createNewUser(String openId, WxOAuth2UserInfo userInfo) {
        log.info("Creating new user account for: {}", openId);

        // Create user
        User user = buildUserFromWechatInfo(userInfo);
        userService.save(user);

        // Create connection
        createThirdPartyConnection(user.getUid(), openId);
    }

    private User buildUserFromWechatInfo(WxOAuth2UserInfo userInfo) {
        Long uid = idService.getNextId(IdBizEnum.USER.getBizType());

        return User.builder()
                .uid(uid)
                .registerTime(LocalDateTime.now())
                .password(PasswordUtils.generateDefaultPassword())
                .name(userInfo != null ? userInfo.getNickname() : null)
                .nickname(userInfo != null ? userInfo.getNickname() : null)
                .avatar(userInfo != null ? userInfo.getHeadImgUrl() : null)
                .status(1)
                .deleted(false)
                .build();
    }

    private void createThirdPartyConnection(Long uid, String openId) {
        Long connectionId = idService.getNextId(IdBizEnum.THIRD_PARTY_CONNECTION.getBizType());

        ThirdPartyConnection connection = ThirdPartyConnection.builder()
                .connectionId(connectionId)
                .uid(uid)
                .thirdPartyUserId(openId)
                .platformType(OAuth2Enum.WECHAT.getName())
                .deleted(0)
                .build();

        thirdPartyConnectionService.save(connection);
    }

    private void updateUserInfo(User user, WxOAuth2UserInfo userInfo) {
        log.debug("Updating user info for: {}", user.getId());

        userService.lambdaUpdate()
                .eq(User::getUid, user.getUid())
                .set(User::getName, userInfo.getNickname())
                .set(User::getNickname, userInfo.getNickname())
                .set(User::getAvatar, userInfo.getHeadImgUrl())
                .update();
    }

    private WxMpXmlOutMessage buildTextMessage(String content, WxMpXmlMessage msg, WxMpService service) {
        try {
            return new TextWechatMpBuilder().build(content, msg, service);
        } catch (Exception e) {
            log.error("Failed to build Wx message", e);
            return null;
        }
    }
}