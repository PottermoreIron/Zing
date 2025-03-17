package com.pot.common.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * @author: Pot
 * @created: 2025/3/16 22:28
 * @description: 结果码枚举
 */
@Getter
public enum ResultCode {
    /**
     * 操作成功
     */
    SUCCESS(200, "操作成功", true),
    /**
     * 请求参数错误
     */
    BAD_REQUEST(400, "请求参数错误", false),
    /**
     * 未授权访问
     */
    UNAUTHORIZED(401, "未授权访问", false),
    /**
     * 禁止访问
     */
    FORBIDDEN(403, "禁止访问", false),
    /**
     * 资源未找到
     */
    NOT_FOUND(404, "资源未找到", false),
    /**
     * 服务器内部错误
     */
    INTERNAL_ERROR(500, "服务器内部错误", false),
    /**
     * 服务不可用
     */
    SERVICE_UNAVAILABLE(503, "服务不可用", false),
    // 业务错误码
    /**
     * 参数校验异常
     */
    PARAM_ERROR(100, "参数校验异常", false),
    /**
     * 手机号不合法
     */
    PHONE_NOT_LEGAL(1000, "手机号不合法", false),
    /**
     * 短信验证码不存在或过期
     */
    SMS_CODE_NOT_EXIST(1001, "短信验证码不存在或过期", false),
    /**
     * 短信验证码错误
     */
    SMS_CODE_ERROR(1002, "短信验证码错误", false),
    /**
     * 用户不存在
     */
    USER_NOT_EXIST(1003, "用户不存在", false),
    /**
     * 用户已存在
     */
    USER_EXIST(1004, "用户已存在", false),
    ;

    private final int code;
    private final String msg;
    private final boolean success;

    ResultCode(int code, String msg, boolean success) {
        this.code = code;
        this.msg = msg;
        this.success = success;
    }

    public static ResultCode getError(int code) {
        return Arrays.stream(ResultCode.values())
                .filter(resultCode -> resultCode.getCode() == code)
                .findFirst()
                .orElse(null);
    }

    public static ResultCode getError(String msg) {
        return Arrays.stream(ResultCode.values())
                .filter(rc -> rc.getMsg().equals(msg))
                .findFirst()
                .orElse(null);
    }
}