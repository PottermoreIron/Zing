package com.pot.auth.domain.authorization.aspect;

import com.pot.auth.domain.authorization.annotation.RequireAnyPermission;
import com.pot.auth.domain.authorization.expression.*;
import com.pot.auth.domain.port.SecurityPort;
import com.pot.auth.domain.shared.enums.Logical;
import com.pot.auth.infrastructure.security.exception.PermissionDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * 任一权限检查切面
 *
 * <p>
 * 拦截标注了{@link RequireAnyPermission}注解的方法，验证用户是否拥有任一权限
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequireAnyPermissionAspect {

    private final PermissionExpressionParser expressionParser;
    private final SecurityPort securityPort;

    @Around("@annotation(com.pot.auth.domain.authorization.annotation.RequireAnyPermission)")
    public Object checkAnyPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireAnyPermission annotation = method.getAnnotation(RequireAnyPermission.class);

        // 1. 获取当前认证上下文
        if (!securityPort.isAuthenticated()) {
            throw new PermissionDeniedException("未登录用户无法访问资源");
        }

        // 2. 获取用户权限集合
        Set<String> userPermissions = securityPort.getCurrentUserPermissions();
        String userId = securityPort.getCurrentUserId();

        // 3. 构建评估上下文
        PermissionExpression.EvaluationContext context = StandardPermissionEvaluationContext.builder()
                .permissions(userPermissions)
                .currentUserId(userId)
                .build();

        // 4. 检查是否拥有任一权限（使用OR逻辑）
        PermissionExpression expression = expressionParser.parseMultiple(
                annotation.value(),
                Logical.OR);

        boolean hasPermission = expression.evaluate(context);

        if (!hasPermission) {
            log.warn("[权限拒绝] 用户 {} 缺少任一权限: {}, 方法: {}",
                    userId, expression.getExpression(), method.getName());
            throw new PermissionDeniedException("权限不足");
        }

        log.debug("[权限通过] 用户 {} 任一权限检查通过: {}", userId, expression.getExpression());
        return joinPoint.proceed();
    }
}
