package com.pot.member.service.handler.wechat;

import com.pot.common.utils.JacksonUtils;
import com.pot.member.service.handler.wechat.builder.TextWechatMpBuilder;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author: Pot
 * @created: 2025/4/13 17:21
 * @description: 默认微信公众号消息处理器
 */
@Component
public class DefaultWechatMpHandler extends AbstractWechatMpHandler {

    @Override
    public String supportedMsgType() {
        return "";
    }

    @Override
    public String supportedEventType() {
        return "";
    }

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage, Map<String, Object> map, WxMpService wxMpService, WxSessionManager wxSessionManager) {
        String content = STR."收到信息内容：\{JacksonUtils.toJson(wxMpXmlMessage.getContent())}. 客服稍后回复您";
        return new TextWechatMpBuilder().build(content, wxMpXmlMessage, wxMpService);
    }
}
