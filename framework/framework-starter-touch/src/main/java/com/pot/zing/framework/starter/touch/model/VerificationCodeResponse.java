package com.pot.zing.framework.starter.touch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author: Pot
 * @created: 2025/10/19 16:49
 * @description: 验证码响应类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerificationCodeResponse {
    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 验证码(测试环境返回,生产环境不返回)
     */
    private String code;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 发送渠道
     */
    private String channelType;
}
