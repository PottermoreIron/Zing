package com.pot.zing.framework.starter.touch.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author: Pot
 * @created: 2025/10/19 16:47
 * @description: 模版枚举
 */
@Getter
@AllArgsConstructor
public enum TemplateType {
    /**
     * 验证码类型
     */
    VERIFICATION_CODE("VERIFICATION_CODE", "验证码"),

    /**
     * 通知类型
     */
    NOTIFICATION("NOTIFICATION", "通知"),

    /**
     * 营销类型
     */
    MARKETING("MARKETING", "营销");

    private final String code;
    private final String description;
}
