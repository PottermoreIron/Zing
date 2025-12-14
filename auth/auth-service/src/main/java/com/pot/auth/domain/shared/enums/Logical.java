package com.pot.auth.domain.shared.enums;

/**
 * 逻辑关系枚举
 *
 * <p>
 * 用于权限、角色注解的逻辑运算和表达式解析
 *
 * @author pot
 * @since 2025-12-14
 */
public enum Logical {
    /**
     * 逻辑与 - 必须满足所有条件
     */
    AND,

    /**
     * 逻辑或 - 满足任意一个条件即可
     */
    OR,

    /**
     * 逻辑非 - 取反（用于复杂表达式）
     */
    NOT
}
