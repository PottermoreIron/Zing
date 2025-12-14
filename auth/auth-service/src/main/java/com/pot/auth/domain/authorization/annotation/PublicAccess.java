package com.pot.auth.domain.authorization.annotation;

import java.lang.annotation.*;

/**
 * 公开访问注解
 *
 * <p>
 * 标注在方法或类上，表示该接口无需权限验证，任何人都可以访问
 *
 * <p>
 * 常用场景：
 * <ul>
 * <li>公开API（如注册、登录）</li>
 * <li>健康检查接口</li>
 * <li>公开文档接口</li>
 * </ul>
 *
 * <p>
 * 示例：
 * 
 * <pre>
 * {@code @PublicAccess}
 * public void publicMethod() {
 *     // 无需权限验证
 * }
 * </pre>
 *
 * @author pot
 * @since 2025-12-14
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublicAccess {

    /**
     * 描述信息
     */
    String value() default "";
}
