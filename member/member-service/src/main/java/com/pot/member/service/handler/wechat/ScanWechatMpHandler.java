package com.pot.member.service.handler.wechat;

import com.pot.member.service.service.wechat.WechatMpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author: Pot
 * @created: 2025/4/12 22:56
 * @description: 微信公众号扫码处理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScanWechatMpHandler extends AbstractWechatMpHandler {
    private final WechatMpService wechatMpService;

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage, Map<String, Object> map, WxMpService wxMpService, WxSessionManager wxSessionManager) throws WxErrorException {
        return wechatMpService.scan(wxMpService, wxMpXmlMessage);
    }

    @Override
    public String supportedMsgType() {
        return WxConsts.XmlMsgType.EVENT;
    }

    @Override
    public String supportedEventType() {
        return WxConsts.EventType.SCAN;
    }
}
