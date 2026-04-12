package com.pot.zing.framework.starter.touch.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Supported touch delivery channels.
 */
@Getter
@AllArgsConstructor
public enum TouchChannelType {
    SMS("sms", "SMS"),
    EMAIL("email", "Email"),
    PUSH("push", "App Push"),
    WECHAT("wechat", "WeChat Message"),
    WEBSOCKET("websocket", "In-App Message"),
    FEISHU("feishu", "Feishu");

    private final String code;
    private final String desc;
}
