package com.pot.member.service.service.wechat;

import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;

/**
 * @author: Pot
 * @created: 2025/4/12 21:49
 * @description: wechat service
 */
public interface WechatMpService {
    /**
     * @param wxMpService    微信公众号服务
     * @param wxMpXmlMessage 微信公众号消息
     * @return WxMpXmlOutMessage
     * @author pot
     * @description
     * @date 16:36 2025/4/13
     **/
    WxMpXmlOutMessage scan(WxMpService wxMpService, WxMpXmlMessage wxMpXmlMessage);

    /**
     * @param userInfo 用户信息
     * @author pot
     * @description
     * @date 16:37 2025/4/13
     **/
    void authorize(WxOAuth2UserInfo userInfo);
}
