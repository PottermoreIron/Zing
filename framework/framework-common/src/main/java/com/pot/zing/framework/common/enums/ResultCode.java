package com.pot.zing.framework.common.enums;

import com.pot.zing.framework.common.service.IResultCode;
import lombok.Getter;

/**
 * @author: Pot
 * @created: 2025/3/16 22:28
 * @description: 结果码枚举
 */
@Getter
public enum ResultCode implements IResultCode {
    /**
     * 操作成功
     */
    SUCCESS("200", "操作成功", true),
    /**
     * 请求参数错误
     */
    BAD_REQUEST("400", "请求参数错误", false),
    /**
     * 未授权访问
     */
    UNAUTHORIZED("401", "未授权访问", false),
    /**
     * 禁止访问
     */
    FORBIDDEN("403", "禁止访问", false),
    /**
     * 资源未找到
     */
    NOT_FOUND("404", "资源未找到", false),
    /**
     * 服务器内部错误
     */
    INTERNAL_ERROR("500", "服务器内部错误", false),
    /**
     * 服务不可用
     */
    SERVICE_UNAVAILABLE("503", "服务不可用", false),
    // 业务错误码
    /**
     * 参数校验异常
     */
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