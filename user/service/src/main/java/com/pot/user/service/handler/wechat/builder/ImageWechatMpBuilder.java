package com.pot.user.service.handler.wechat.builder;

import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;

/**
 * @author: Pot
 * @created: 2025/4/13 15:36
 * @description: 微信公众号图片消息builder
 */
public class ImageWechatMpBuilder extends AbstractWechatMpBuilder {
    @Override
    public WxMpXmlOutMessage build(String content, WxMpXmlMessage wxMessage, WxMpService service) {
        return WxMpXmlOutMessage.IMAGE().mediaId(content)
                .fromUser(wxMessage.getToUser()).toUser(wxMessage.getFromUser())
                .build();
    }
}
