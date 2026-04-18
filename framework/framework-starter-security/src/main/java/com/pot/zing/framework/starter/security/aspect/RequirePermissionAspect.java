package com.pot.zing.framework.starter.security.aspect;

import com.pot.zing.framework.starter.security.annotation.RequirePermission;
import com.pot.zing.framework.starter.security.enums.Logical;
import com.pot.zing.framework.starter.security.exception.PermissionDeniedException;
import com.pot.zing.framework.starter.security.expression.PermissionExpression;
import com.pot.zing.framework.starter.security.expression.PermissionExpressionParser;
import com.pot.zing.framework.starter.security.port.SecurityContextPort;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.lang.reflect.Method;

/**
 * Aspect that enforces {@link RequirePermission} contracts.
 */
@Slf4j
@Aspect
public class RequirePermissionAspect extends AuthorizationAspectSupport {

    private final PermissionExpressionParser expressionParser;

    public RequirePermissionAspect(PermissionExpressionParser expressionParser, SecurityContextPort securityContextPort) {
        super(securityContextPort);
        this.expressionParser = expressionParser;
    }

    @Around("@annotation(com.pot.zing.framework.starter.security.annotation.RequirePermission) || "
            + "@within(com.pot.zing.framework.starter.security.annotation.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        RequirePermission annotation = findAnnotation(joinPoint, RequirePermission.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }
        requireAuthenticated();

        Logical operator = annotation.logical() == Logical.AND ? Logical.AND : Logical.OR;
        PermissionExpression expression = expressionParser.parseMultiple(annotation.value(), operator);
        if (!expression.evaluate(buildEvaluationContext(joinPoint))) {
            Method method = resolveMethod(joinPoint);
            log.warn("[Access Denied] userId={} lacks permission: {}, method: {}",
                    securityContextPort.getCurrentUserId(), expression.getExpression(), method.getName());
            throw new PermissionDeniedException(annotation.message());
        }
        return joinPoint.proceed();
    }
}
