package com.pot.zing.framework.mq.core;

/**
 * 消息消费者接口
 *
 * <p>
 * 统一的消息消费抽象
 *
 * @param <T> 消息类型
 * @author Copilot
 * @since 2026-01-05
 */
@FunctionalInterface
public interface MessageConsumer<T> {

    /**
     * 处理消息
     *
     * @param message 消息内容
     */
    void consume(T message);
}
