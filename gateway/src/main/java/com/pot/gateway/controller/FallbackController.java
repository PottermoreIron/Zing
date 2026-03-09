package com.pot.gateway.controller;

import com.pot.zing.framework.common.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 熔断降级 Fallback 控制器
 *
 * <p>
 * 当下游服务触发熔断（CircuitBreaker OPEN）时，
 * Gateway 会将请求 forward 到此控制器，返回统一的友好错误响应，
 * 避免长时间等待或 502 Bad Gateway。
 *
 * <p>
 * 路由配置示例：
 * 
 * <pre>
 * filters:
 *   - name: CircuitBreaker
 *     args:
 *       name: auth-service-cb
 *       fallbackUri: forward:/fallback
 * </pre>
 *
 * @author pot
 * @since 2026-03-09
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * 统一降级响应
     *
     * <p>
     * 通过 {@code ServerWebExchange} 可获取原始请求路径、
     * 以及熔断器设置的异常信息（key: {@code circuitBreaker.failureReason}），
     * 便于日志排查。
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
