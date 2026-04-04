package com.pot.zing.framework.starter.authorization.aspect;

import com.pot.zing.framework.starter.authorization.exception.PermissionDeniedException;
import com.pot.zing.framework.starter.authorization.expression.PermissionExpression;
import com.pot.zing.framework.starter.authorization.expression.StandardPermissionEvaluationContext;
import com.pot.zing.framework.starter.authorization.security.AuthorizationSecurityAccessor;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * 授权切面公共支持。
 */
@RequiredArgsConstructor
abstract class AuthorizationAspectSupport {

    protected final AuthorizationSecurityAccessor securityAccessor;
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    protected void requireAuthenticated() {
        if (!securityAccessor.isAuthenticated()) {
            throw new PermissionDeniedException("未登录用户无法访问资源");
        }
    }

    protected PermissionExpression.EvaluationContext buildEvaluationContext(ProceedingJoinPoint joinPoint) {
        return StandardPermissionEvaluationContext.builder()
                .permissions(securityAccessor.getCurrentUserPermissions())
                .roles(securityAccessor.getCurrentUserRoles())
                .currentUserId(securityAccessor.getCurrentUserId())
                .methodParameters(buildMethodParameters(joinPoint, joinPoint.getArgs()))
                .build();
    }

    protected Method resolveMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = joinPoint.getTarget() != null
                ? AopUtils.getTargetClass(joinPoint.getTarget())
                : signature.getDeclaringType();
        return AopUtils.getMostSpecificMethod(signature.getMethod(), targetClass);
    }

    protected <A extends Annotation> A findAnnotation(ProceedingJoinPoint joinPoint, Class<A> annotationType) {
        Method method = resolveMethod(joinPoint);
        A methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, annotationType);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }

        Class<?> targetClass = joinPoint.getTarget() != null
                ? AopUtils.getTargetClass(joinPoint.getTarget())
                : method.getDeclaringClass();
        return AnnotatedElementUtils.findMergedAnnotation(targetClass, annotationType);
    }

    private Map<String, Object> buildMethodParameters(ProceedingJoinPoint joinPoint, Object[] args) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Map<String, Object> parameters = new HashMap<>();
        if (args == null) {
            return parameters;
        }

        Method method = resolveMethod(joinPoint);
        String[] signatureParameterNames = signature.getParameterNames();
        String[] discoveredParameterNames = parameterNameDiscoverer.getParameterNames(method);
        Parameter[] reflectionParameters = method.getParameters();

        for (int index = 0; index < args.length; index++) {
            Object argument = args[index];
            parameters.put("p" + index, argument);
            parameters.put("a" + index, argument);
            putIfPresent(parameters, resolveParameterName(signatureParameterNames, index), argument);
            putIfPresent(parameters, resolveParameterName(discoveredParameterNames, index), argument);
            if (index < reflectionParameters.length) {
                String reflectionName = reflectionParameters[index].getName();
                if (!isSyntheticName(reflectionName)) {
                    parameters.put(reflectionName, argument);
                }
            }
        }
        return parameters;
    }

    private String resolveParameterName(String[] parameterNames, int index) {
        if (parameterNames == null || index >= parameterNames.length) {
            return null;
        }
        String parameterName = parameterNames[index];
        return isSyntheticName(parameterName) ? null : parameterName;
    }

    private boolean isSyntheticName(String parameterName) {
        return parameterName == null || parameterName.matches("arg\\d+");
    }

    private void putIfPresent(Map<String, Object> parameters, String parameterName, Object argument) {
        if (parameterName != null) {
            parameters.put(parameterName, argument);
        }
    }
}