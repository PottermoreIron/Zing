package com.pot.user.service.aop;

import com.pot.common.enums.ResultCode;
import com.pot.user.service.annotations.ratelimit.RateLimit;
import com.pot.user.service.enums.ratelimit.LimitPolicy;
import com.pot.user.service.enums.ratelimit.RateLimitType;
import com.pot.user.service.exception.RateLimitException;
import com.pot.user.service.ratelimit.RateLimitKeyProvider;
import com.pot.user.service.ratelimit.RateLimitManager;
import com.pot.user.service.ratelimit.RateLimitProperties;
import com.pot.user.service.ratelimit.impl.FixedRateLimitKeyProvider;
import com.pot.user.service.ratelimit.impl.GuavaRateLimitManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: Pot
 * @created: 2025/3/30 22:24
 * @description: 限流切面类
 */
@Aspect
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "ratelimit", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitAspect {
    private final RateLimitManager rateLimitManager;
    private final Map<RateLimitType, RateLimitKeyProvider> keyProviders;
    private final RateLimitProperties properties;

    public RateLimitAspect(
            ObjectProvider<RateLimitManager> rateLimitManagerProvider,
            ObjectProvider<RateLimitKeyProvider> keyProvidersProvider,
            RateLimitProperties properties) {
        // 使用ObjectProvider处理可能的依赖不存在情况
        this.rateLimitManager = rateLimitManagerProvider.getIfAvailable(() ->
                new GuavaRateLimitManager(properties.getExpireAfterAccess()));

        // 收集所有注册的KeyProvider
        this.keyProviders = keyProvidersProvider.stream()
                .collect(Collectors.toMap(RateLimitKeyProvider::getType, Function.identity()));

        this.properties = properties;
    }

    @Around("@annotation(com.pot.user.service.annotations.ratelimit.RateLimit)")
    public Object handleRateLimit(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = AnnotationUtils.findAnnotation(method, RateLimit.class);

        if (rateLimit == null || !properties.isEnabled()) {
            return pjp.proceed();
        }

        // 根据限流类型获取最终的key
        String finalKey = getFinalKey(rateLimit, method, pjp);

        // 获取实际速率，支持通过配置文件覆盖注解值
        double rate = getActualRate(rateLimit, finalKey);

        // 尝试获取令牌
        boolean acquired = rateLimitManager.tryAcquire(
                finalKey,
                rate,
                rateLimit.policy() == LimitPolicy.WAIT ? rateLimit.waitTimeout() : 0,
                TimeUnit.MILLISECONDS
        );

        if (acquired) {
            return pjp.proceed();
        } else {
            throw new RateLimitException(ResultCode.RATE_LIMIT_EXCEPTION);
        }
    }

    /**
     * 获取限流的key
     */
    private String getFinalKey(RateLimit rateLimit, Method method, ProceedingJoinPoint pjp) {
        String baseKey = Optional.ofNullable(rateLimit.key())
                .filter(s -> !s.isEmpty())
                .orElse(method.getDeclaringClass().getName() + "." + method.getName() + "." +
                        Arrays.stream(pjp.getArgs())
                                .map(Object::toString)
                                .collect(Collectors.joining(",")));
        log.error("!!!!!pot:{}", baseKey);

        // 添加全局前缀，便于统一管理
        baseKey = properties.getKeyPrefix() + baseKey;

        // 获取对应类型的提供者，如果不存在则使用固定类型提供者
        RateLimitKeyProvider provider = keyProviders.getOrDefault(
                rateLimit.type(),
                keyProviders.getOrDefault(RateLimitType.FIXED,
                        new FixedRateLimitKeyProvider()));

        return provider.getKey(baseKey, pjp, rateLimit);
    }

    /**
     * 获取实际速率，支持配置文件覆盖
     */
    private double getActualRate(RateLimit rateLimit, String key) {
        // 检查配置文件中是否有为特定key配置的速率
        Map<String, Double> rateOverrides = properties.getRateOverrides();
        if (rateOverrides != null && rateOverrides.containsKey(key)) {
            return rateOverrides.get(key);
        }

        // 使用全局速率因子调整注解速率
        return rateLimit.count() * properties.getGlobalRateFactor();
    }
}
