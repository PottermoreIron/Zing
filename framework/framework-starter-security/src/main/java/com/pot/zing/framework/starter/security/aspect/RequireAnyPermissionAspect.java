package com.pot.zing.framework.starter.security.aspect;

import com.pot.zing.framework.starter.security.annotation.RequireAnyPermission;
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
 * Aspect that enforces {@link RequireAnyPermission} contracts.
 */
@Slf4j
@Aspect
public class RequireAnyPermissionAspect extends AuthorizationAspectSupport {

    private final PermissionExpressionParser expressionParser;

    public RequireAnyPermissionAspect(PermissionExpressionParser expressionParser, SecurityContextPort securityContextPort) {
        super(securityContextPort);
        this.expressionParser = expressionParser;
    }

    @Around("@annotation(com.pot.zing.framework.starter.security.annotation.RequireAnyPermission) || "
            + "@within(com.pot.zing.framework.starter.security.annotation.RequireAnyPermission)")
    public Object checkAnyPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        RequireAnyPermission annotation = findAnnotation(joinPoint, RequireAnyPermission.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }
        requireAuthenticated();

        PermissionExpression expression = expressionParser.parseMultiple(annotation.value(), Logical.OR);
        if (!expression.evaluate(buildEvaluationContext(joinPoint))) {
            Method method = resolveMethod(joinPoint);
            log.warn("[Access Denied] userId={} lacks any of: {}, method: {}",
                    securityContextPort.getCurrentUserId(), expression.getExpression(), method.getName());
            throw new PermissionDeniedException(annotation.message());
        }
        return joinPoint.proceed();
    }
}
