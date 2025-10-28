package com.pot.zing.framework.starter.touch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author: Pot
 * @created: 2025/10/19 15:36
 * @description: 触达结果
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TouchResponse {
    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 渠道类型
     */
    private String channelType;
}
