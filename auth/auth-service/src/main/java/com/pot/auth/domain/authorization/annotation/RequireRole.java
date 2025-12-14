package com.pot.auth.domain.authorization.annotation;

import com.pot.auth.domain.shared.enums.Logical;

import java.lang.annotation.*;

/**
 * 需要角色注解
 *
 * <p>
 * 标注在方法或类上，表示需要指定角色才能访问
 *
 * <p>
 * 示例：
 * <ul>
 * <li>{@code @RequireRole("ADMIN")} - 单个角色</li>
 * <li>{@code @RequireRole({"ADMIN", "MANAGER"})} - 多角色（默认OR）</li>
 * <li>{@code @RequireRole(value = {"ADMIN", "MANAGER"}, logical = Logical.AND)}
 * - 多角色（AND）</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * 需要的角色
     */
    String[] value();

    /**
     * 多角色之间的逻辑关系
     */
    Logical logical() default Logical.OR;

    /**
     * 错误消息
     */
    String message() default "角色权限不足";
}
