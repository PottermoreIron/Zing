package com.pot.auth.domain.authorization.annotation;

import java.lang.annotation.*;

/**
 * 需要任一权限注解
 *
 * <p>
 * 标注在方法上，表示需要任意一个指定权限即可访问
 *
 * <p>
 * 等价于 {@code @RequirePermission(value = {...}, logical = Logical.OR)}
 *
 * <p>
 * 示例：
 * 
 * <pre>
 * {@code @RequireAnyPermission({"user.read", "user.write"})}
 * public void someMethod() {
 *     // 拥有 user.read 或 user.write 任一权限即可访问
 * }
 * </pre>
 *
 * @author pot
 * @since 2025-12-14
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAnyPermission {

    /**
     * 需要的权限（任一即可）
     */
    String[] value();

    /**
     * 错误消息
     */
    String message() default "权限不足";
}
