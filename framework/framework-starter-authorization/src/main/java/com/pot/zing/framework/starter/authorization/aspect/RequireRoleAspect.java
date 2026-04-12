package com.pot.zing.framework.starter.authorization.aspect;

import com.pot.zing.framework.starter.authorization.annotation.RequireRole;
import com.pot.zing.framework.starter.authorization.exception.PermissionDeniedException;
import com.pot.zing.framework.starter.authorization.security.AuthorizationSecurityAccessor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

/**
 * Aspect that enforces role requirements.
 */
@Slf4j
@Aspect
public class RequireRoleAspect extends AuthorizationAspectSupport {

    public RequireRoleAspect(AuthorizationSecurityAccessor securityAccessor) {
        super(securityAccessor);
    }

    @Around("@annotation(com.pot.zing.framework.starter.authorization.annotation.RequireRole) || "
            + "@within(com.pot.zing.framework.starter.authorization.annotation.RequireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        RequireRole annotation = findAnnotation(joinPoint, RequireRole.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        requireAuthenticated();

        Set<String> userRoles = securityAccessor.getCurrentUserRoles();
        boolean hasRole = switch (annotation.logical()) {
            case AND -> Arrays.stream(annotation.value()).allMatch(userRoles::contains);
            case OR -> Arrays.stream(annotation.value()).anyMatch(userRoles::contains);
            case NOT -> throw new UnsupportedOperationException("NOT logic not supported for roles");
        };
        if (!hasRole) {
            Method method = resolveMethod(joinPoint);
            log.warn("[Access Denied] User {} lacks required role: {}, method: {}",
                    securityAccessor.getCurrentUserId(), Arrays.toString(annotation.value()), method.getName());
            throw new PermissionDeniedException(annotation.message());
        }

        return joinPoint.proceed();
    }
}