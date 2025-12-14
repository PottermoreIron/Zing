package com.pot.auth.domain.authorization.annotation;

import com.pot.auth.domain.shared.enums.Logical;

import java.lang.annotation.*;

/**
 * 需要权限注解
 *
 * <p>
 * 标注在方法或类上，表示需要指定权限才能访问
 *
 * <p>
 * 支持：
 * <ul>
 * <li>简单权限：{@code @RequirePermission("user.read")}</li>
 * <li>多权限（AND）：{@code @RequirePermission(value = {"user.read", "user.write"},
 * logical = Logical.AND)}</li>
 * <li>多权限（OR）：{@code @RequirePermission(value = {"user.read", "user.write"},
 * logical = Logical.OR)}</li>
 * <li>SpEL表达式：{@code @RequirePermission("hasPermission(#id, 'article', 'edit')")}</li>
 * <li>复杂表达式：{@code @RequirePermission("(user.read AND user.write) OR role.admin")}</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 需要的权限（支持SpEL表达式）
     *
     * <p>
     * 示例：
     * <ul>
     * <li>{@code "user.read"} - 简单权限</li>
     * <li>{@code "article:#id:edit"} - 资源级权限</li>
     * <li>{@code "hasPermission(#articleId, 'article', 'edit')"} - SpEL表达式</li>
     * <li>{@code "(user.read AND user.write) OR role.admin"} - 复杂表达式</li>
     * </ul>
     */
    String[] value();

    /**
     * 多权限之间的逻辑关系
     */
    Logical logical() default Logical.AND;

    /**
     * 错误消息
     */
    String message() default "权限不足";
}
