package com.pot.auth.infrastructure.aspect;

import com.pot.auth.domain.authorization.annotation.RequirePermission;
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
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 权限检查切面（基础设施层）
 *
 * <p>
 * 拦截标注了 {@link RequirePermission} 注解的方法，执行权限验证。
 * AOP 切面属于基础设施横切关注点，不应放在领域层。
 *
 * @author pot
 * @since 2026-03-22
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequirePermissionAspect {

    private final PermissionExpressionParser expressionParser;
    private final SecurityPort securityPort;

    @Around("@annotation(com.pot.auth.domain.authorization.annotation.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);

        if (!securityPort.isAuthenticated()) {
            throw new PermissionDeniedException("未登录用户无法访问资源");
        }

        Set<String> userPermissions = securityPort.getCurrentUserPermissions();
        String userId = securityPort.getCurrentUserId();

        Map<String, Object> methodParams = buildMethodParameters(signature, joinPoint.getArgs());

        PermissionExpression.EvaluationContext context = StandardPermissionEvaluationContext.builder()
                .permissions(userPermissions)
                .currentUserId(userId)
                .methodParameters(methodParams)
                .build();

        Logical operator = annotation.logical() == Logical.AND ? Logical.AND : Logical.OR;
        PermissionExpression expression = expressionParser.parseMultiple(annotation.value(), operator);
        boolean hasPermission = expression.evaluate(context);

        if (!hasPermission) {
            log.warn("[权限拒绝] 用户 {} 缺少权限: {}, 方法: {}", userId, expression.getExpression(), method.getName());
            throw new PermissionDeniedException(annotation.message());
        }

        log.debug("[权限通过] 用户 {} 通过权限检查: {}", userId, expression.getExpression());
        return joinPoint.proceed();
    }

    private Map<String, Object> buildMethodParameters(MethodSignature signature, Object[] args) {
        Parameter[] parameters = signature.getMethod().getParameters();
        Map<String, Object> paramMap = new HashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            paramMap.put(parameters[i].getName(), args[i]);
        }
        return paramMap;
    }
}
