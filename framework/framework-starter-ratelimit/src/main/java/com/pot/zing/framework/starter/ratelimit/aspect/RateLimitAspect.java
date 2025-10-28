package com.pot.zing.framework.starter.ratelimit.aspect;

import com.pot.zing.framework.starter.ratelimit.annotation.RateLimit;
import com.pot.zing.framework.starter.ratelimit.enums.RateLimitMethodEnum;
import com.pot.zing.framework.starter.ratelimit.enums.RateLimitPolicyEnum;
import com.pot.zing.framework.starter.ratelimit.exception.RateLimitException;
import com.pot.zing.framework.starter.ratelimit.properties.RateLimitProperties;
import com.pot.zing.framework.starter.ratelimit.service.RateLimitKeyProvider;
import com.pot.zing.framework.starter.ratelimit.service.RateLimitManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author: Pot
 * @created: 2025/10/18 22:07
 * @description: 限流切面
 */
@Aspect
@Slf4j
public class RateLimitAspect {

    private final RateLimitManager rateLimitManager;
    private final List<RateLimitKeyProvider> keyProviders;
    private final RateLimitProperties properties;

    public RateLimitAspect(
            RateLimitManager rateLimitManager,
            List<RateLimitKeyProvider> keyProviders,
            RateLimitProperties properties) {
        this.rateLimitManager = rateLimitManager;
        this.keyProviders = keyProviders.stream()
                .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
                .collect(Collectors.toList());
        this.properties = properties;
    }

    @Around("@annotation(com.pot.zing.framework.starter.ratelimit.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = AnnotationUtils.findAnnotation(method, RateLimit.class);

        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        // 生成限流key
        String rateLimitKey = generateRateLimitKey(joinPoint, method, rateLimit);

        // 尝试获取令牌
        boolean acquired = tryAcquireToken(rateLimitKey, rateLimit);

        if (acquired) {
            log.debug("限流检查通过 - key: {}", rateLimitKey);
            return joinPoint.proceed();
        } else {
            log.warn("限流触发 - key: {}, rate: {}", rateLimitKey, rateLimit.rate());
            throw new RateLimitException(rateLimit.message());
        }
    }

    /**
     * 生成限流key
     */
    private String generateRateLimitKey(ProceedingJoinPoint joinPoint, Method method, RateLimit rateLimit) {
        // 基础key：优先使用注解中的key，否则使用方法签名
        String baseKey = StringUtils.hasText(rateLimit.key())
                ? rateLimit.key()
                : generateMethodSignatureKey(joinPoint, method);

        // 添加全局前缀
        baseKey = properties.getKeyPrefix() + hashKey(baseKey);

        // 根据限流类型生成最终key
        RateLimitKeyProvider provider = findKeyProvider(rateLimit);
        return provider.generateKey(baseKey, joinPoint, rateLimit);
    }

    /**
     * 生成方法签名key
     */
    private String generateMethodSignatureKey(ProceedingJoinPoint joinPoint, Method method) {
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        String args = Arrays.stream(joinPoint.getArgs())
                .map(arg -> arg != null ? arg.toString() : "null")
                .collect(Collectors.joining(","));

        return String.format("%s.%s(%s)", className, methodName, args);
    }

    /**
     * key哈希处理
     */
    private String hashKey(String key) {
        return DigestUtils.md5DigestAsHex(key.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 查找对应的key提供者
     */
    private RateLimitKeyProvider findKeyProvider(RateLimit rateLimit) {
        return keyProviders.stream()
                .filter(provider -> provider.getSupportedType() == rateLimit.type())
                .findFirst()
                .orElseGet(() -> keyProviders.stream()
                        .filter(provider -> provider.getSupportedType() == RateLimitMethodEnum.FIXED)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("未找到限流key提供者")));
    }

    /**
     * 尝试获取令牌
     */
    private boolean tryAcquireToken(String key, RateLimit rateLimit) {
        long timeout = rateLimit.policy() == RateLimitPolicyEnum.WAIT ? rateLimit.waitTimeout() : 0;
        return rateLimitManager.tryAcquire(key, rateLimit.rate(), timeout, TimeUnit.MILLISECONDS);
    }
}
