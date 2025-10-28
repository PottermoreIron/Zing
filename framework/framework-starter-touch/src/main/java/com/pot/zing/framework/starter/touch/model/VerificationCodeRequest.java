package com.pot.zing.framework.starter.touch.model;

import com.pot.zing.framework.starter.touch.enums.TouchChannelType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * @author: Pot
 * @created: 2025/10/19 16:48
 * @description: 验证码请求
 */
@Data
@Builder
public class VerificationCodeRequest {
    /**
     * 接收目标(手机号/邮箱)
     */
    private String target;

    /**
     * 渠道类型
     */
    private TouchChannelType channelType;

    /**
     * 验证码长度,默认6位
     */
    @Builder.Default
    private Integer codeLength = 6;

    /**
     * 验证码有效期(秒),默认300秒
     */
    @Builder.Default
    private Long expireSeconds = 300L;

    /**
     * 业务类型(注册/登录/找回密码等)
     */
    private String bizType;

    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;
}
