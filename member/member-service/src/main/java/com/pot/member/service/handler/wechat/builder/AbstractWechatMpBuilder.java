package com.pot.member.service.handler.wechat.builder;

import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;

/**
 * @author: Pot
 * @created: 2025/4/13 15:35
 * @description: 微信公众号消息抽象builder
 */
public abstract class AbstractWechatMpBuilder {
    public abstract WxMpXmlOutMessage build(String content,
                                            WxMpXmlMessage wxMessage, WxMpService service);
}
