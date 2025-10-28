package com.pot.zing.framework.security.annotation;

import java.lang.annotation.*;

/**
 * 需要角色注解
 * <p>
 * 标注在方法上，表示需要指定角色才能访问
 * </p>
 *
 * @author Pot
 * @since 2025-01-24
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {

    /**
     * 需要的角色
     */
    String[] value();

    /**
     * 逻辑关系（AND/OR）
     */
    Logical logical() default Logical.AND;

    enum Logical {
        /**
         * 必须拥有所有角色
         */
        AND,
        /**
         * 拥有任意一个角色即可
         */
        OR
    }
}

