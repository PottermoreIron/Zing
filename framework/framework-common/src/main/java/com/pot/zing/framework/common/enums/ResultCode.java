package com.pot.zing.framework.common.enums;

import com.pot.zing.framework.common.service.IResultCode;
import lombok.Getter;

/**
 * Standard framework result codes.
 */
@Getter
public enum ResultCode implements IResultCode {
    SUCCESS("200", "操作成功", true),
    BAD_REQUEST("400", "请求参数错误", false),
    UNAUTHORIZED("401", "未授权访问", false),
    FORBIDDEN("403", "禁止访问", false),
    NOT_FOUND("404", "资源未找到", false),
    INTERNAL_ERROR("500", "服务器内部错误", false),
    SERVICE_UNAVAILABLE("503", "服务不可用", false),

    PARAM_ERROR("PARAM_100", "参数校验异常", false);

    private final String code;
    private final String msg;
    private final boolean success;

    ResultCode(String code, String msg, boolean success) {
        this.code = code;
        this.msg = msg;
        this.success = success;
    }
}