package com.pot.zing.framework.starter.security.aspect;

import com.pot.zing.framework.starter.security.exception.PermissionDeniedException;
import com.pot.zing.framework.starter.security.expression.PermissionExpression;
import com.pot.zing.framework.starter.security.expression.StandardPermissionEvaluationContext;
import com.pot.zing.framework.starter.security.port.SecurityContextPort;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared support for authorization aspects.
 *
 * <p>All access to the security context goes through {@link SecurityContextPort},
 * keeping aspects decoupled from any concrete security framework.</p>
 */
@RequiredArgsConstructor
abstract class AuthorizationAspectSupport {

    protected final SecurityContextPort securityContextPort;
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    protected void requireAuthenticated() {
        if (!securityContextPort.isAuthenticated()) {
            throw new PermissionDeniedException("Unauthenticated user cannot access the resource");
        }
    }

    protected PermissionExpression.EvaluationContext buildEvaluationContext(ProceedingJoinPoint joinPoint) {
        return StandardPermissionEvaluationContext.builder()
                .permissions(securityContextPort.getCurrentUserPermissions())
                .roles(securityContextPort.getCurrentUserRoles())
                .currentUserId(securityContextPort.getCurrentUserId())
                .methodParameters(buildMethodParameters(joinPoint))
                .build();
    }

    protected Method resolveMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = joinPoint.getTarget() != null
                ? AopUtils.getTargetClass(joinPoint.getTarget())
                : signature.getDeclaringType();
        return AopUtils.getMostSpecificMethod(signature.getMethod(), targetClass);
    }

    protected <A extends Annotation> A findAnnotation(ProceedingJoinPoint joinPoint, Class<A> type) {
        Method method = resolveMethod(joinPoint);
        A annotation = AnnotatedElementUtils.findMergedAnnotation(method, type);
        if (annotation != null) {
            return annotation;
        }
        Class<?> targetClass = joinPoint.getTarget() != null
                ? AopUtils.getTargetClass(joinPoint.getTarget())
                : method.getDeclaringClass();
        return AnnotatedElementUtils.findMergedAnnotation(targetClass, type);
    }

    private Map<String, Object> buildMethodParameters(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        Map<String, Object> parameters = new HashMap<>();
        if (args == null) {
            return parameters;
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = resolveMethod(joinPoint);
        String[] signatureNames = signature.getParameterNames();
        String[] discoveredNames = parameterNameDiscoverer.getParameterNames(method);
        Parameter[] reflectionParams = method.getParameters();

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            parameters.put("p" + i, arg);
            parameters.put("a" + i, arg);
            putIfPresent(parameters, resolveParameterName(signatureNames, i), arg);
            putIfPresent(parameters, resolveParameterName(discoveredNames, i), arg);
            if (i < reflectionParams.length) {
                String name = reflectionParams[i].getName();
                if (!isSyntheticName(name)) {
                    parameters.put(name, arg);
                }
            }
        }
        return parameters;
    }

    private String resolveParameterName(String[] names, int index) {
        if (names == null || index >= names.length) return null;
        String name = names[index];
        return isSyntheticName(name) ? null : name;
    }

    private boolean isSyntheticName(String name) {
        return name == null || name.matches("arg\\d+");
    }

    private void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (key != null) map.put(key, value);
    }
}
