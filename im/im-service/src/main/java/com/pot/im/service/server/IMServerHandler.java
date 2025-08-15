package com.pot.im.service.server;

import com.pot.im.service.message.MessageProcessor;
import com.pot.im.service.message.MessageProcessorFactory;
import com.pot.im.service.protocol.serializer.MessageType;
import com.pot.im.service.protocol.serializer.ProtocolMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author: Pot
 * @created: 2025/8/10 23:20
 * @description: IM服务器核心处理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IMServerHandler extends SimpleChannelInboundHandler<ProtocolMessage> {

    private final ConnectionManager connectionManager;
    private final MessageProcessorFactory processorFactory;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        connectionManager.addConnection(ctx.channel());
        log.debug("Client connected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        connectionManager.removeConnection(ctx.channel().id());
        log.debug("Client disconnected: {}", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProtocolMessage message) {
        try {
            MessageType messageType = MessageType.fromCode(message.getHeader().getMsgType());
            processMessage(ctx, message, messageType);
        } catch (Exception e) {
            handleProcessingError(ctx, message, e);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event && event.state() == IdleState.READER_IDLE) {
            handleIdleTimeout(ctx);
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Channel exception [{}]: {}",
                ctx.channel().remoteAddress(), cause.getMessage());
        ctx.close();
    }

    private void processMessage(ChannelHandlerContext ctx, ProtocolMessage message, MessageType messageType) {
        Optional.ofNullable(processorFactory.getPrimaryProcessor(messageType))
                .ifPresentOrElse(
                        processor -> executeProcessor(ctx, message, processor),
                        () -> log.warn("No processor found for message type: {}", messageType)
                );
    }

    private void executeProcessor(ChannelHandlerContext ctx, ProtocolMessage message, MessageProcessor processor) {
        if (processor.isAsync()) {
            CompletableFuture.runAsync(() -> safeProcess(ctx, message, processor),
                            processorFactory.getAsyncExecutor())
                    .exceptionally(throwable -> {
                        log.error("Async processing failed", throwable);
                        return null;
                    });
        } else {
            safeProcess(ctx, message, processor);
        }
    }

    private void safeProcess(ChannelHandlerContext ctx, ProtocolMessage message, MessageProcessor processor) {
        try {
            processor.process(ctx, message);
        } catch (MessageProcessor.ProcessingException e) {
            log.error("Message processing failed: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error in message processing", e);
            throw e;
        }
    }

    private void handleProcessingError(ChannelHandlerContext ctx, ProtocolMessage message, Exception e) {
        log.error("Error processing message [type: {}, from: {}]: {}",
                message.getHeader().getMsgType(),
                ctx.channel().remoteAddress(),
                e.getMessage());

        // 根据错误类型决定是否关闭连接
        if (isRecoverableError(e)) {
            log.info("Recoverable error, keeping connection alive");
        } else {
            log.warn("Fatal error, closing connection");
            ctx.close();
        }
    }

    private void handleIdleTimeout(ChannelHandlerContext ctx) {
        Optional.ofNullable(connectionManager.getChannelUser(ctx.channel().id()))
                .ifPresentOrElse(
                        userId -> log.info("User {} idle timeout, closing connection", userId),
                        () -> log.info("Anonymous client idle timeout: {}", ctx.channel().remoteAddress())
                );
        ctx.close();
    }

    private boolean isRecoverableError(Exception e) {
        return e instanceof MessageProcessor.ProcessingException ||
                e instanceof IllegalArgumentException;
    }
}
