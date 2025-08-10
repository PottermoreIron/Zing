package com.pot.im.service.server;

import com.pot.im.service.protocol.serializer.MessageType;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author: Pot
 * @created: 2025/8/10 23:08
 * @description: 消息处理器工厂
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageProcessorFactory {

    private final List<MessageProcessor> processors;
    private final Map<MessageType, List<MessageProcessor>> processorMap = new ConcurrentHashMap<>();
    /**
     * -- GETTER --
     * 获取异步执行器
     */
    @Getter
    private final Executor asyncExecutor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "MessageProcessor-Async");
        thread.setDaemon(true);
        return thread;
    });

    @PostConstruct
    public void init() {
        log.info("Initializing MessageProcessorFactory with {} processors", processors.size());

        processors.forEach(this::registerProcessor);

        log.info("MessageProcessor registration completed: {}",
                processorMap.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().size())));
    }

    /**
     * 获取消息处理器列表(按优先级排序)
     */
    public List<MessageProcessor> getProcessors(MessageType messageType) {
        return processorMap.getOrDefault(messageType, Collections.emptyList());
    }

    /**
     * 获取主要处理器(优先级最高的处理器)
     */
    public MessageProcessor getPrimaryProcessor(MessageType messageType) {
        return getProcessors(messageType).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No processor found for type: " + messageType));
    }

    private void registerProcessor(MessageProcessor processor) {
        Arrays.stream(processor.getSupportedTypes())
                .forEach(type -> processorMap
                        .computeIfAbsent(type, k -> new ArrayList<>())
                        .add(processor));

        // 按优先级排序
        processorMap.values().forEach(list ->
                list.sort(Comparator.comparingInt(MessageProcessor::getPriority)));
    }
}