package com.pot.auth.domain.authorization.aspect;

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
 * 权限检查切面
 *
 * <p>
 * 拦截标注了{@link RequirePermission}注解的方法，执行权限验证：
 * <ul>
 * <li>解析权限表达式</li>
 * <li>构建评估上下文（用户权限、方法参数）</li>
 * <li>评估权限表达式</li>
 * <li>权限不足时抛出异常</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
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

        // 1. 获取当前认证上下文
        if (!securityPort.isAuthenticated()) {
            throw new PermissionDeniedException("未登录用户无法访问资源");
        }

        // 2. 获取用户权限集合
        Set<String> userPermissions = securityPort.getCurrentUserPermissions();
        String userId = securityPort.getCurrentUserId();

        // 3. 构建方法参数映射
        Map<String, Object> methodParams = buildMethodParameters(signature, joinPoint.getArgs());

        // 4. 构建评估上下文
        PermissionExpression.EvaluationContext context = StandardPermissionEvaluationContext.builder()
                .permissions(userPermissions)
                .currentUserId(userId)
                .methodParameters(methodParams)
                .build();

        // 5. 解析并评估权限表达式
        Logical operator = annotation.logical() == Logical.AND
                ? Logical.AND
                : Logical.OR;

        PermissionExpression expression = expressionParser.parseMultiple(
                annotation.value(),
                operator);

        boolean hasPermission = expression.evaluate(context);

        if (!hasPermission) {
            log.warn("[权限拒绝] 用户 {} 缺少权限: {}, 方法: {}",
                    userId, expression.getExpression(), method.getName());
            throw new PermissionDeniedException(annotation.message());
        }

        log.debug("[权限通过] 用户 {} 通过权限检查: {}", userId, expression.getExpression());
        return joinPoint.proceed();
    }

    /**
     * 构建方法参数映射
     */
    private Map<String, Object> buildMethodParameters(MethodSignature signature, Object[] args) {
        Parameter[] parameters = signature.getMethod().getParameters();
        Map<String, Object> paramMap = new HashMap<>();

        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            paramMap.put(paramName, args[i]);
        }

        return paramMap;
    }
}
