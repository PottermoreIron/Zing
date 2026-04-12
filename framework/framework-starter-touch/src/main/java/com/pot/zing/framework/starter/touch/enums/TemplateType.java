package com.pot.zing.framework.starter.touch.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Template categories used by touch channels.
 */
@Getter
@AllArgsConstructor
public enum TemplateType {
    VERIFICATION_CODE("VERIFICATION_CODE", "Verification Code"),

    NOTIFICATION("NOTIFICATION", "Notification"),

    MARKETING("MARKETING", "Marketing");

    private final String code;
    private final String description;
}
