package com.pot.zing.framework.starter.touch.service.impl;

import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.touch.exception.TouchException;
import com.pot.zing.framework.starter.touch.model.TouchRequest;
import com.pot.zing.framework.starter.touch.model.TouchResponse;
import com.pot.zing.framework.starter.touch.service.TouchChannel;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * @author: Pot
 * @created: 2025/10/19 16:07
 * @description: 抽象触达类实现
 */
@Slf4j
public abstract class AbstractTouchChannelImpl implements TouchChannel {

    @Override
    public R<TouchResponse> send(TouchRequest request) {
        try {
            // 1. 前置校验
            preValidate(request);

            // 2. 参数校验
            validateRequest(request);

            // 3. 限流检查
            checkRateLimit(request);

            // 4. 执行发送
            String messageId = doSend(request);

            // 5. 构建响应
            TouchResponse response = buildResponse(messageId, request);

            // 6. 后置处理
            postProcess(request, response);

            return R.success(response);

        } catch (TouchException e) {
            log.error("消息发送失败: channel={}, target={}, error={}",
                    getChannelType(), request.getTarget(), e.getMessage());
            return R.fail(e.getMessage());
        } catch (Exception e) {
            log.error("消息发送异常: channel={}, target={}",
                    getChannelType(), request.getTarget(), e);
            return R.fail("消息发送失败: " + e.getMessage());
        }
    }

    /**
     * 前置校验(可选)
     */
    protected void preValidate(TouchRequest request) {
        // 子类可覆盖
    }

    /**
     * 参数校验(必须实现)
     */
    protected abstract void validateRequest(TouchRequest request);

    /**
     * 限流检查(可选)
     */
    protected void checkRateLimit(TouchRequest request) {
        // 子类可实现限流逻辑
    }

    /**
     * 执行发送(必须实现)
     */
    protected abstract String doSend(TouchRequest request);

    /**
     * 构建响应
     */
    protected TouchResponse buildResponse(String messageId, TouchRequest request) {
        return TouchResponse.builder()
                .messageId(messageId)
                .sendTime(LocalDateTime.now())
                .channelType(getChannelType().getCode())
                .build();
    }

    /**
     * 后置处理(可选)
     */
    protected void postProcess(TouchRequest request, TouchResponse response) {
        // 子类可实现审计日志、监控上报等
    }
}
