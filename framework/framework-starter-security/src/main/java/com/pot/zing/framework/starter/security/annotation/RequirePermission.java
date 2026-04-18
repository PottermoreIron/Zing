package com.pot.zing.framework.starter.security.annotation;

import com.pot.zing.framework.starter.security.enums.Logical;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the current user to satisfy the configured permissions.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    String[] value();

    Logical logical() default Logical.AND;

    String message() default "Insufficient permissions";
}
