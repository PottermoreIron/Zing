package com.pot.im.service.message;

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

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageProcessorFactory {

    private final List<MessageProcessor> processors;
    private final Map<MessageType, List<MessageProcessor>> processorMap = new ConcurrentHashMap<>();
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

        public List<MessageProcessor> getProcessors(MessageType messageType) {
        return processorMap.getOrDefault(messageType, Collections.emptyList());
    }

        public MessageProcessor getPrimaryProcessor(MessageType messageType) {
        return getProcessors(messageType).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No processor found for type: " + messageType));
    }

    private void registerProcessor(MessageProcessor processor) {
        Arrays.stream(processor.getSupportedTypes())
                .forEach(type -> processorMap
                        .computeIfAbsent(type, k -> new ArrayList<>())
                        .add(processor));

        processorMap.values().forEach(list ->
                list.sort(Comparator.comparingInt(MessageProcessor::getPriority)));
    }
}