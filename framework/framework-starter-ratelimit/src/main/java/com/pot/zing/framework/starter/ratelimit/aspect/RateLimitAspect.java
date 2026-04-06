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
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
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
 * Aspect that enforces method-level rate limits.
 */
@Aspect
@Slf4j
public class RateLimitAspect {

    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

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

        String rateLimitKey = generateRateLimitKey(joinPoint, method, rateLimit);

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
     * Builds the effective rate-limit key.
     */
    private String generateRateLimitKey(ProceedingJoinPoint joinPoint, Method method, RateLimit rateLimit) {
        String baseKey = resolveBaseKey(joinPoint, method, rateLimit);

        baseKey = properties.getKeyPrefix() + hashKey(baseKey);

        RateLimitKeyProvider provider = findKeyProvider(rateLimit);
        return provider.generateKey(baseKey, joinPoint, rateLimit);
    }

    private String resolveBaseKey(ProceedingJoinPoint joinPoint, Method method, RateLimit rateLimit) {
        if (!StringUtils.hasText(rateLimit.key())) {
            return generateMethodSignatureKey(joinPoint, method);
        }
        if (!looksLikeSpelExpression(rateLimit.key())) {
            return rateLimit.key().trim();
        }
        return evaluateKeyExpression(joinPoint, method, rateLimit.key());
    }

    private boolean looksLikeSpelExpression(String keyExpression) {
        return keyExpression.contains("#") || keyExpression.contains("T(") || keyExpression.contains("@{")
                || keyExpression.contains("@") || keyExpression.contains("[");
    }

    private String evaluateKeyExpression(ProceedingJoinPoint joinPoint, Method method, String keyExpression) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
            evaluationContext.setVariable("args", args);
            evaluationContext.setVariable("methodName", method.getName());
            evaluationContext.setVariable("className", method.getDeclaringClass().getName());

            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                evaluationContext.setVariable("p" + i, arg);
                evaluationContext.setVariable("a" + i, arg);
                evaluationContext.setVariable("arg" + i, arg);
                if (parameterNames != null && i < parameterNames.length && StringUtils.hasText(parameterNames[i])) {
                    evaluationContext.setVariable(parameterNames[i], arg);
                }
            }

            Object evaluated = SPEL_PARSER.parseExpression(keyExpression).getValue(evaluationContext);
            if (evaluated == null || !StringUtils.hasText(evaluated.toString())) {
                throw new IllegalStateException("限流SpEL表达式返回空值: " + keyExpression);
            }
            return evaluated.toString();
        } catch (Exception e) {
            throw new IllegalStateException("解析限流key表达式失败: " + keyExpression, e);
        }
    }

    /**
     * Falls back to a signature-based key when no explicit key is configured.
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
     * Hashes the key to keep Redis and cache keys compact.
     */
    private String hashKey(String key) {
        return DigestUtils.md5DigestAsHex(key.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Resolves the matching key provider and falls back to the fixed provider.
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
     * Attempts to acquire a token according to the configured policy.
     */
    private boolean tryAcquireToken(String key, RateLimit rateLimit) {
        long timeout = rateLimit.policy() == RateLimitPolicyEnum.WAIT ? rateLimit.waitTimeout() : 0;
        return rateLimitManager.tryAcquire(key, rateLimit.rate(), timeout, TimeUnit.MILLISECONDS);
    }
}
