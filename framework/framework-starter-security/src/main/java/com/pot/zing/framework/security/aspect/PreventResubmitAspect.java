package com.pot.zing.framework.security.aspect;

import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.security.annotation.PreventResubmit;
import com.pot.zing.framework.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交切面
 * <p>
 * 实现@PreventResubmit注解功能
 * </p>
 *
 * @author Pot
 * @since 2025-01-24
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PreventResubmitAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String RESUBMIT_PREFIX = "security:resubmit:";

    @Around("@annotation(com.pot.zing.framework.security.annotation.PreventResubmit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        PreventResubmit annotation = method.getAnnotation(PreventResubmit.class);

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            // 未登录用户不做防重复提交检查
            return joinPoint.proceed();
        }

        // 生成唯一键
        String key = RESUBMIT_PREFIX + userId + ":" + method.getDeclaringClass().getName() + ":" + method.getName();

        // 尝试设置键
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", annotation.interval(), TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(success)) {
            log.warn("防重复提交拦截: userId={}, method={}", userId, method.getName());
            throw new BusinessException(annotation.message());
        }

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            // 如果执行失败，删除键，允许重试
            redisTemplate.delete(key);
            throw e;
        }
    }
}

