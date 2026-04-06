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

@Component
@Slf4j
public class ConnectionManager {

    private final ConcurrentMap<ChannelId, Channel> channels = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Channel> userChannels = new ConcurrentHashMap<>();
    private final ConcurrentMap<ChannelId, String> channelUsers = new ConcurrentHashMap<>();
    private final AtomicLong connectionCount = new AtomicLong(0);

        public void addConnection(Channel channel) {
        Objects.requireNonNull(channel, "Channel cannot be null");

        if (channels.putIfAbsent(channel.id(), channel) == null) {
            long count = connectionCount.incrementAndGet();
            log.info("Connection established: {} [Total: {}]", channel.id(), count);
        }
    }

        public void removeConnection(ChannelId channelId) {
        Channel channel = channels.remove(channelId);
        if (channel != null) {
            cleanupUserMapping(channelId);
            long count = connectionCount.decrementAndGet();
            log.info("Connection removed: {} [Total: {}]", channelId, count);
        }
    }

        public void bindUser(String userId, Channel channel) {
        Objects.requireNonNull(userId, "UserId cannot be null");
        Objects.requireNonNull(channel, "Channel cannot be null");

        handleUserReconnection(userId, channel);

        userChannels.put(userId, channel);
        channelUsers.put(channel.id(), userId);

        log.info("User bound: {} -> {}", userId, channel.id());
    }

        public Channel getUserChannel(String userId) {
        return Optional.ofNullable(userChannels.get(userId))
                .filter(Channel::isActive)
                .orElse(null);
    }

        public String getChannelUser(ChannelId channelId) {
        return channelUsers.get(channelId);
    }

        public boolean isUserOnline(String userId) {
        return getUserChannel(userId) != null;
    }

        public Set<String> getOnlineUsers() {
        return Set.copyOf(userChannels.keySet());
    }

        public ConnectionStats getStats() {
        return ConnectionStats.builder()
                .totalConnections(connectionCount.get())
                .authenticatedUsers(userChannels.size())
                .build();
    }

        public void broadcast(Object message) {
        broadcast(message, null);
    }

        public void broadcast(Object message, Consumer<String> userFilter) {
        userChannels.entrySet().parallelStream()
                .filter(entry -> entry.getValue().isActive())
                .filter(entry -> userFilter == null || filterUser(entry.getKey(), userFilter))
                .forEach(entry -> entry.getValue().writeAndFlush(message));
    }

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