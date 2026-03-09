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
 * 全链路追踪 TraceId 过滤器
 *
 * <p>
 * 职责：
 * <ul>
 * <li>若请求中已携带 {@code X-Trace-Id} 则透传，否则生成新的 UUID TraceId</li>
 * <li>将 TraceId 写入转发给下游服务的请求 Header</li>
 * <li>将 TraceId 写入响应 Header，方便客户端关联日志</li>
 * <li>在 MDC 中设置 traceId，使日志自动携带（可选）</li>
 * </ul>
 *
 * <p>
 * 优先级：{@code -200}，在鉴权过滤器（{@code -100}）之前执行，
 * 确保后续所有过滤器的日志都能带上 TraceId。
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
        // 1. 提取或生成 TraceId
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }

        final String finalTraceId = traceId;

        // 2. 注入到下游请求 Header
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(TRACE_ID_HEADER, finalTraceId)
                .build();

        // 3. 注入到响应 Header（让客户端可拿到 traceId 用于反馈排查）
        exchange.getResponse().getHeaders().add(TRACE_ID_HEADER, finalTraceId);

        log.debug("[TraceId] traceId={}, path={}", finalTraceId,
                exchange.getRequest().getURI().getPath());

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        // 最高优先级：所有过滤器执行前先打上 TraceId
        return -200;
    }

    /**
     * 生成无连字符的 32 位 UUID TraceId
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
