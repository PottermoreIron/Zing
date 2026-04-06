package com.pot.gateway.controller;

import com.pot.zing.framework.common.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Returns a unified fallback response when a downstream circuit breaker opens.
 *
 * @author pot
 * @since 2026-03-09
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * Returns the standardized gateway fallback payload.
     */
    @RequestMapping
    public Mono<R<Void>> fallback(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        Object failureReason = exchange.getAttribute("circuitBreaker.failureReason");

        log.warn("[熔断降级] 服务不可用: path={}, reason={}", path, failureReason);

        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        return Mono.just(R.fail("服务暂时不可用，请稍后重试"));
    }
}
