package com.pot.zing.framework.starter.authorization.aspect;

import com.pot.zing.framework.starter.authorization.annotation.RequireAnyPermission;
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
 * Aspect that enforces any-of permission requirements.
 */
@Slf4j
@Aspect
public class RequireAnyPermissionAspect extends AuthorizationAspectSupport {

    private final PermissionExpressionParser expressionParser;

    public RequireAnyPermissionAspect(
            PermissionExpressionParser expressionParser,
            AuthorizationSecurityAccessor securityAccessor) {
        super(securityAccessor);
        this.expressionParser = expressionParser;
    }

    @Around("@annotation(com.pot.zing.framework.starter.authorization.annotation.RequireAnyPermission) || "
            + "@within(com.pot.zing.framework.starter.authorization.annotation.RequireAnyPermission)")
    public Object checkAnyPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        RequireAnyPermission annotation = findAnnotation(joinPoint, RequireAnyPermission.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        requireAuthenticated();

        PermissionExpression expression = expressionParser.parseMultiple(annotation.value(), Logical.OR);
        if (!expression.evaluate(buildEvaluationContext(joinPoint))) {
            Method method = resolveMethod(joinPoint);
            log.warn("[权限拒绝] 用户 {} 缺少任一权限: {}, 方法: {}",
                    securityAccessor.getCurrentUserId(), expression.getExpression(), method.getName());
            throw new PermissionDeniedException(annotation.message());
        }

        return joinPoint.proceed();
    }
}