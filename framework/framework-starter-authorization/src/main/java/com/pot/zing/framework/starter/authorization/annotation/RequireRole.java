package com.pot.zing.framework.starter.authorization.annotation;

import com.pot.zing.framework.starter.authorization.enums.Logical;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the current user to satisfy the configured roles.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    String[] value();

    Logical logical() default Logical.OR;

    String message() default "Insufficient role privileges";
}