package com.pot.zing.framework.starter.security.aspect;

import com.pot.zing.framework.starter.security.annotation.RequireRole;
import com.pot.zing.framework.starter.security.exception.PermissionDeniedException;
import com.pot.zing.framework.starter.security.port.SecurityContextPort;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

/**
 * Aspect that enforces {@link RequireRole} contracts.
 */
@Slf4j
@Aspect
public class RequireRoleAspect extends AuthorizationAspectSupport {

    public RequireRoleAspect(SecurityContextPort securityContextPort) {
        super(securityContextPort);
    }

    @Around("@annotation(com.pot.zing.framework.starter.security.annotation.RequireRole) || "
            + "@within(com.pot.zing.framework.starter.security.annotation.RequireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        RequireRole annotation = findAnnotation(joinPoint, RequireRole.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }
        requireAuthenticated();

        Set<String> userRoles = securityContextPort.getCurrentUserRoles();
        boolean hasRole = switch (annotation.logical()) {
            case AND -> Arrays.stream(annotation.value()).allMatch(userRoles::contains);
            case OR  -> Arrays.stream(annotation.value()).anyMatch(userRoles::contains);
            case NOT -> throw new UnsupportedOperationException("NOT logic is not supported for roles");
        };
        if (!hasRole) {
            Method method = resolveMethod(joinPoint);
            log.warn("[Access Denied] userId={} lacks role: {}, method: {}",
                    securityContextPort.getCurrentUserId(), Arrays.toString(annotation.value()), method.getName());
            throw new PermissionDeniedException(annotation.message());
        }
        return joinPoint.proceed();
    }
}
