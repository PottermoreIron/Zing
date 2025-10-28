package com.pot.zing.framework.starter.touch.model;

import com.pot.zing.framework.starter.touch.enums.TouchChannelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author: Pot
 * @created: 2025/10/19 15:27
 * @description: 触达请求
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TouchRequest {
    /**
     * 接收目标(手机号/邮箱/用户ID)
     */
    private String target;

    /**
     * 渠道类型
     */
    private TouchChannelType channelType;

    /**
     * 消息模板ID
     */
    private String templateId;

    /**
     * 模板参数
     */
    private Map<String, Object> params;

    /**
     * 业务类型(用于审计)
     */
    private String bizType;
}
