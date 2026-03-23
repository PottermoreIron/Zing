package com.pot.auth.infrastructure.aspect;

import com.pot.auth.domain.authorization.annotation.RequireRole;
import com.pot.auth.domain.port.SecurityPort;
import com.pot.auth.infrastructure.security.exception.PermissionDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

/**
 * 角色检查切面（基础设施层）
 *
 * <p>
 * 拦截标注了 {@link RequireRole} 注解的方法，执行角色验证。
 * AOP 切面属于基础设施横切关注点，不应放在领域层。
 *
 * @author pot
 * @since 2026-03-22
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequireRoleAspect {

    private final SecurityPort securityPort;

    @Around("@annotation(com.pot.auth.domain.authorization.annotation.RequireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireRole annotation = method.getAnnotation(RequireRole.class);

        if (!securityPort.isAuthenticated()) {
            throw new PermissionDeniedException("未登录用户无法访问资源");
        }

        Set<String> userRoles = securityPort.getCurrentUserRoles();
        String userId = securityPort.getCurrentUserId();
        String[] requiredRoles = annotation.value();

        boolean hasRole = switch (annotation.logical()) {
            case AND -> Arrays.stream(requiredRoles).allMatch(userRoles::contains);
            case OR -> Arrays.stream(requiredRoles).anyMatch(userRoles::contains);
            case NOT -> throw new UnsupportedOperationException("NOT logic not supported for roles");
        };

        if (!hasRole) {
            log.warn("[权限拒绝] 用户 {} 缺少角色: {}, 方法: {}", userId, Arrays.toString(requiredRoles), method.getName());
            throw new PermissionDeniedException(annotation.message());
        }

        log.debug("[权限通过] 用户 {} 角色检查通过: {}", userId, Arrays.toString(requiredRoles));
        return joinPoint.proceed();
    }
}
