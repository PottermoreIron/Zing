package com.pot.auth.domain.authorization.annotation;

import java.lang.annotation.*;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublicAccess {

        String value() default "";
}
