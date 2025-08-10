package com.pot.im.service.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * @author: Pot
 * @created: 2025/8/10 23:10
 * @description: 连接管理器 - 管理所有客户端连接
 */
@Component
@Slf4j
public class ConnectionManager {

    private final ConcurrentMap<ChannelId, Channel> channels = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Channel> userChannels = new ConcurrentHashMap<>();
    private final ConcurrentMap<ChannelId, String> channelUsers = new ConcurrentHashMap<>();
    private final AtomicLong connectionCount = new AtomicLong(0);

    /**
     * 添加连接
     */
    public void addConnection(Channel channel) {
        Objects.requireNonNull(channel, "Channel cannot be null");

        if (channels.putIfAbsent(channel.id(), channel) == null) {
            long count = connectionCount.incrementAndGet();
            log.info("Connection established: {} [Total: {}]", channel.id(), count);
        }
    }

    /**
     * 移除连接并清理关联数据
     */
    public void removeConnection(ChannelId channelId) {
        Channel channel = channels.remove(channelId);
        if (channel != null) {
            cleanupUserMapping(channelId);
            long count = connectionCount.decrementAndGet();
            log.info("Connection removed: {} [Total: {}]", channelId, count);
        }
    }

    /**
     * 绑定用户到连接
     */
    public void bindUser(String userId, Channel channel) {
        Objects.requireNonNull(userId, "UserId cannot be null");
        Objects.requireNonNull(channel, "Channel cannot be null");

        // 处理用户重复登录
        handleUserReconnection(userId, channel);

        userChannels.put(userId, channel);
        channelUsers.put(channel.id(), userId);

        log.info("User bound: {} -> {}", userId, channel.id());
    }

    /**
     * 获取用户连接
     */
    public Channel getUserChannel(String userId) {
        return Optional.ofNullable(userChannels.get(userId))
                .filter(Channel::isActive)
                .orElse(null);
    }

    /**
     * 获取连接对应用户
     */
    public String getChannelUser(ChannelId channelId) {
        return channelUsers.get(channelId);
    }

    /**
     * 检查用户在线状态
     */
    public boolean isUserOnline(String userId) {
        return getUserChannel(userId) != null;
    }

    /**
     * 获取在线用户集合
     */
    public Set<String> getOnlineUsers() {
        return Set.copyOf(userChannels.keySet());
    }

    /**
     * 获取连接统计信息
     */
    public ConnectionStats getStats() {
        return ConnectionStats.builder()
                .totalConnections(connectionCount.get())
                .authenticatedUsers(userChannels.size())
                .build();
    }

    /**
     * 广播消息给所有在线用户
     */
    public void broadcast(Object message) {
        broadcast(message, null);
    }

    /**
     * 条件广播
     */
    public void broadcast(Object message, Consumer<String> userFilter) {
        userChannels.entrySet().parallelStream()
                .filter(entry -> entry.getValue().isActive())
                .filter(entry -> userFilter == null || filterUser(entry.getKey(), userFilter))
                .forEach(entry -> entry.getValue().writeAndFlush(message));
    }

    /**
     * 发送消息给指定用户
     */
    public boolean sendToUser(String userId, Object message) {
        return Optional.ofNullable(getUserChannel(userId))
                .map(channel -> {
                    channel.writeAndFlush(message);
                    return true;
                })
                .orElseGet(() -> {
                    log.warn("User {} is offline, message not sent", userId);
                    return false;
                });
    }

    private void cleanupUserMapping(ChannelId channelId) {
        Optional.ofNullable(channelUsers.remove(channelId))
                .ifPresent(userId -> {
                    userChannels.remove(userId);
                    log.info("User disconnected: {}", userId);
                });
    }

    private void handleUserReconnection(String userId, Channel newChannel) {
        Optional.ofNullable(userChannels.get(userId))
                .filter(oldChannel -> !oldChannel.equals(newChannel))
                .ifPresent(oldChannel -> {
                    channelUsers.remove(oldChannel.id());
                    oldChannel.close();
                    log.info("User reconnected, old connection closed: {}", userId);
                });
    }

    private boolean filterUser(String userId, Consumer<String> filter) {
        try {
            filter.accept(userId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Builder
    public record ConnectionStats(long totalConnections, int authenticatedUsers) {
    }
}