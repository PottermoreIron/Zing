package com.pot.member.service.enums;

import lombok.Getter;

/**
 * @author: Pot
 * @created: 2025/4/6 22:02
 * @description: 分布式id枚举
 */
@Getter
public enum IdBizEnum {
    USER("user", "用户id"),
    THIRD_PARTY_CONNECTION("third_party_connection", "第三方连接id");
    private final String bizType;
    private final String description;

    IdBizEnum(String bizType, String description) {
        this.bizType = bizType;
        this.description = description;
    }
}
