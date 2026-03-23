package com.pot.auth.infrastructure.aspect;

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
 * 任一权限检查切面（基础设施层）
 *
 * <p>
 * 拦截标注了 {@link RequireAnyPermission} 注解的方法，验证用户是否拥有任一指定权限。
 * AOP 切面属于基础设施横切关注点，不应放在领域层。
 *
 * @author pot
 * @since 2026-03-22
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

        if (!securityPort.isAuthenticated()) {
            throw new PermissionDeniedException("未登录用户无法访问资源");
        }

        Set<String> userPermissions = securityPort.getCurrentUserPermissions();
        String userId = securityPort.getCurrentUserId();

        PermissionExpression.EvaluationContext context = StandardPermissionEvaluationContext.builder()
                .permissions(userPermissions)
                .currentUserId(userId)
                .build();

        PermissionExpression expression = expressionParser.parseMultiple(annotation.value(), Logical.OR);
        boolean hasPermission = expression.evaluate(context);

        if (!hasPermission) {
            log.warn("[权限拒绝] 用户 {} 缺少任一权限: {}, 方法: {}", userId, expression.getExpression(), method.getName());
            throw new PermissionDeniedException("权限不足");
        }

        log.debug("[权限通过] 用户 {} 任一权限检查通过: {}", userId, expression.getExpression());
        return joinPoint.proceed();
    }
}
