package com.pot.zing.framework.security.annotation;

import java.lang.annotation.*;

/**
 * 防重复提交注解
 * <p>
 * 标注在方法上，防止用户重复提交表单
 * </p>
 *
 * @author Pot
 * @since 2025-01-24
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PreventResubmit {

    /**
     * 防重复提交的时间间隔（秒）
     */
    int interval() default 5;

    /**
     * 提示消息
     */
    String message() default "操作过于频繁，请稍后再试";
}

