package com.pot.zing.framework.starter.touch.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Template categories used by touch channels.
 */
@Getter
@AllArgsConstructor
public enum TemplateType {
    VERIFICATION_CODE("VERIFICATION_CODE", "验证码"),

    NOTIFICATION("NOTIFICATION", "通知"),

    MARKETING("MARKETING", "营销");

    private final String code;
    private final String description;
}
