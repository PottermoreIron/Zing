package com.pot.member.service.handler.wechat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author: Pot
 * @created: 2025/4/13 17:01
 * @description: 微信公众号取消关注处理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnsubscribeWechatMpHandler extends AbstractWechatMpHandler {
    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage, Map<String, Object> map, WxMpService wxMpService, WxSessionManager wxSessionManager) {
        String openId = wxMpXmlMessage.getFromUser();
        log.info("取消关注用户 OPENID:{}", openId);
        // TODO 更新数据库
        return null;
    }

    @Override
    public String supportedMsgType() {
        return WxConsts.XmlMsgType.EVENT;
    }

    @Override
    public String supportedEventType() {
        return WxConsts.EventType.UNSUBSCRIBE;
    }
}
