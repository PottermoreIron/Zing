package com.pot.zing.framework.starter.touch.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author: Pot
 * @created: 2025/10/19 15:27
 * @description: 触达渠道枚举
 */
@Getter
@AllArgsConstructor
public enum TouchChannelType {
    SMS("sms", "短信"),
    EMAIL("email", "邮件"),
    PUSH("push", "App推送"),
    WECHAT("wechat", "微信消息"),
    WEBSOCKET("websocket", "站内信"),
    FEISHU("feishu", "飞书");

    private final String code;
    private final String desc;
}
