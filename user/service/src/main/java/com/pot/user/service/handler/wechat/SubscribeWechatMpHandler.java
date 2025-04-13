package com.pot.user.service.handler.wechat;

import com.pot.user.service.handler.wechat.builder.TextWechatMpBuilder;
import com.pot.user.service.service.wechat.WechatMpService;
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
 * @created: 2025/4/12 22:56
 * @description: 微信公众号订阅处理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscribeWechatMpHandler extends AbstractWechatMpHandler {
    private final WechatMpService wechatMpService;

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage, Map<String, Object> map, WxMpService wxMpService, WxSessionManager wxSessionManager) {
        log.info("新用户关注公众号，OPENID:{}", wxMpXmlMessage.getFromUser());
        WxMpXmlOutMessage responseResult;
        try {
            responseResult = this.handleSpecial(wxMpService, wxMpXmlMessage);
            return responseResult == null ? new TextWechatMpBuilder().build("感谢关注", wxMpXmlMessage, wxMpService) : responseResult;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 处理特殊请求，比如如果是扫码进来的，可以做相应处理
     */
    private WxMpXmlOutMessage handleSpecial(WxMpService wxMpService, WxMpXmlMessage wxMessage) {
        return wechatMpService.scan(wxMpService, wxMessage);
    }

    @Override
    public String supportedMsgType() {
        return WxConsts.XmlMsgType.EVENT;
    }

    @Override
    public String supportedEventType() {
        return WxConsts.EventType.SUBSCRIBE;
    }
}
