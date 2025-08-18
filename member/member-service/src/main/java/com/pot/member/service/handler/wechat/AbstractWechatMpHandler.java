package com.pot.member.service.handler.wechat;

import me.chanjar.weixin.mp.api.WxMpMessageHandler;

/**
 * @author: Pot
 * @created: 2025/4/12 22:54
 * @description: 抽象微信公众号处理器
 */
public abstract class AbstractWechatMpHandler implements WxMpMessageHandler {
    /**
     * @return String
     * @author pot
     * @description
     * @date 17:17 2025/4/13
     **/
    public abstract String supportedMsgType();

    /**
     * @return String
     * @author pot
     * @description
     * @date 17:17 2025/4/13
     **/
    public abstract String supportedEventType();
}
