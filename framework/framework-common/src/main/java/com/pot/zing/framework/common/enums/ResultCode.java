package com.pot.zing.framework.common.enums;

import com.pot.zing.framework.common.service.IResultCode;
import lombok.Getter;

/**
 * Standard framework result codes.
 */
@Getter
public enum ResultCode implements IResultCode {
    SUCCESS("200", "Operation successful", true),
    BAD_REQUEST("400", "Invalid request parameters", false),
    UNAUTHORIZED("401", "Unauthorized access", false),
    FORBIDDEN("403", "Access forbidden", false),
    NOT_FOUND("404", "Resource not found", false),
    RATE_LIMIT_EXCEEDED("429", "Too many requests, please try again later", false),
    INTERNAL_ERROR("500", "Internal server error", false),
    SERVICE_UNAVAILABLE("503", "Service unavailable", false),

    PARAM_ERROR("PARAM_100", "Parameter validation failed", false);

    private final String code;
    private final String msg;
    private final boolean success;

    ResultCode(String code, String msg, boolean success) {
        this.code = code;
        this.msg = msg;
        this.success = success;
    }
}