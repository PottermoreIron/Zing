package com.pot.zing.framework.starter.authorization.aspect;

import com.pot.zing.framework.starter.authorization.annotation.RequirePermission;
import com.pot.zing.framework.starter.authorization.enums.Logical;
import com.pot.zing.framework.starter.authorization.exception.PermissionDeniedException;
import com.pot.zing.framework.starter.authorization.expression.PermissionExpression;
import com.pot.zing.framework.starter.authorization.expression.PermissionExpressionParser;
import com.pot.zing.framework.starter.authorization.security.AuthorizationSecurityAccessor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.lang.reflect.Method;

/**
 * 权限检查切面。
 */
@Slf4j
@Aspect
public class RequirePermissionAspect extends AuthorizationAspectSupport {

    private final PermissionExpressionParser expressionParser;

    public RequirePermissionAspect(
            PermissionExpressionParser expressionParser,
            AuthorizationSecurityAccessor securityAccessor) {
        super(securityAccessor);
        this.expressionParser = expressionParser;
    }

    @Around("@annotation(com.pot.zing.framework.starter.authorization.annotation.RequirePermission) || "
            + "@within(com.pot.zing.framework.starter.authorization.annotation.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        RequirePermission annotation = findAnnotation(joinPoint, RequirePermission.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        requireAuthenticated();

        PermissionExpression.EvaluationContext context = buildEvaluationContext(joinPoint);
        Logical operator = annotation.logical() == Logical.AND ? Logical.AND : Logical.OR;
        PermissionExpression expression = expressionParser.parseMultiple(annotation.value(), operator);
        if (!expression.evaluate(context)) {
            Method method = resolveMethod(joinPoint);
            log.warn("[权限拒绝] 用户 {} 缺少权限: {}, 方法: {}",
                    securityAccessor.getCurrentUserId(), expression.getExpression(), method.getName());
            throw new PermissionDeniedException(annotation.message());
        }

        return joinPoint.proceed();
    }
}