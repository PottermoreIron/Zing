package com.pot.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Ensures every request carries a trace identifier before later gateway filters
 * run.
 *
 * <p>
 * The filter reuses an incoming {@code X-Trace-Id} when present, otherwise it
 * generates one and propagates it to both downstream requests and client
 * responses.
 *
 * @author pot
 * @since 2026-03-09
 */
@Slf4j
@Component
public class TraceIdGatewayFilter implements GlobalFilter, Ordered {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }

        final String finalTraceId = traceId;

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(TRACE_ID_HEADER, finalTraceId)
                .build();

        exchange.getResponse().getHeaders().add(TRACE_ID_HEADER, finalTraceId);

        log.debug("[TraceId] traceId={}, path={}", finalTraceId,
                exchange.getRequest().getURI().getPath());

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -200;
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
