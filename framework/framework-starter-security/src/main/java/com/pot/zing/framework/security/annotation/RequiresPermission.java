package com.pot.zing.framework.security.annotation;

import java.lang.annotation.*;

/**
 * 需要权限注解
 * <p>
 * 标注在方法上，表示需要指定权限才能访问
 * </p>
 *
 * @author Pot
 * @since 2025-01-24
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * 需要的权限
     */
    String[] value();

    /**
     * 逻辑关系（AND/OR）
     */
    Logical logical() default Logical.AND;

    enum Logical {
        /**
         * 必须拥有所有权限
         */
        AND,
        /**
         * 拥有任意一个权限即可
         */
        OR
    }
}

