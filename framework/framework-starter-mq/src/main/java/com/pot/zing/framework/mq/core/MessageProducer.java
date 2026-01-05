package com.pot.zing.framework.mq.core;

/**
 * 消息生产者接口
 *
 * <p>
 * 统一的消息发送抽象，支持RabbitMQ和Kafka的切换
 *
 * @author Copilot
 * @since 2026-01-05
 */
public interface MessageProducer {

    /**
     * 发送消息到指定主题
     *
     * @param topic   主题/队列名称
     * @param message 消息内容
     */
    void send(String topic, Object message);

    /**
     * 发送消息到指定主题，带路由键（RabbitMQ）或分区键（Kafka）
     *
     * @param topic      主题/队列名称
     * @param routingKey 路由键/分区键
     * @param message    消息内容
     */
    void send(String topic, String routingKey, Object message);

    /**
     * 发送消息到指定主题，带确认回调
     *
     * @param topic    主题/队列名称
     * @param message  消息内容
     * @param callback 确认回调
     */
    void sendWithConfirm(String topic, Object message, PublishCallback callback);
}
