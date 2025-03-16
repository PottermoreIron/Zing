package com.pot.common;

import com.pot.common.enums.ResultCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * @author: Pot
 * @created: 2025/3/16 22:34
 * @description: 结果类
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class R<T> {
    T data;
    String msg;
    Integer code;
    boolean success;

    public R(ResultCode resultCode) {
        this(resultCode, null, resultCode.getMsg());
    }

    public R(ResultCode resultCode, T data) {
        this(resultCode, data, resultCode.getMsg());
    }

    public R(ResultCode resultCode, String msg) {
        this(resultCode, null, msg);
    }

    public R(ResultCode resultCode, T data, String msg) {
        this(data, msg, resultCode.getCode(), resultCode.isSuccess());
    }

    /**
     * 静态工厂方法统一入口
     */
    public static <T> R<T> of(ResultCode resultCode) {
        return new R<>(resultCode);
    }

    public static <T> R<T> of(ResultCode resultCode, T data) {
        return new R<>(resultCode, data);
    }

    public static <T> R<T> of(ResultCode resultCode, String msg) {
        return new R<>(resultCode, msg);
    }

    public static <T> R<T> of(ResultCode resultCode, T data, String msg) {
        return new R<>(resultCode, data, msg);
    }

    /**
     * 快速成功/失败方法
     */
    public static <T> R<T> success() {
        return of(ResultCode.SUCCESS);
    }

    public static <T> R<T> success(T data) {
        return of(ResultCode.SUCCESS, data);
    }

    public static <T> R<T> success(String msg) {
        return of(ResultCode.SUCCESS, msg);
    }

    public static <T> R<T> success(T data, String msg) {
        return of(ResultCode.SUCCESS, data, msg);
    }

    public static <T> R<T> fail() {
        return of(ResultCode.INTERNAL_ERROR);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return of(resultCode);
    }

    public static <T> R<T> fail(String msg) {
        return of(ResultCode.INTERNAL_ERROR, msg);
    }

    public static <T> R<T> fail(T data) {
        return of(ResultCode.INTERNAL_ERROR, data);
    }

    public static <T> R<T> fail(String msg, T data) {
        return of(ResultCode.INTERNAL_ERROR, data, msg);
    }

    public static <T> R<T> fail(ResultCode resultCode, String msg) {
        return of(resultCode, msg);
    }
}
