package com.pot.zing.framework.common.model;

import com.pot.zing.framework.common.enums.ResultCode;
import com.pot.zing.framework.common.service.IResultCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Generic API result wrapper.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class R<T> {
    T data;
    String msg;
    String code;
    boolean success;

    public R(IResultCode resultCode) {
        this(resultCode, null, resultCode.getMsg());
    }

    public R(IResultCode resultCode, T data) {
        this(resultCode, data, resultCode.getMsg());
    }

    public R(IResultCode resultCode, String msg) {
        this(resultCode, null, msg);
    }

    public R(IResultCode resultCode, T data, String msg) {
        this(data, msg, resultCode.getCode(), resultCode.isSuccess());
    }

    /**
     * Creates a result from the given result code.
     */
    public static <T> R<T> of(IResultCode resultCode) {
        return new R<>(resultCode);
    }

    public static <T> R<T> of(IResultCode resultCode, T data) {
        return new R<>(resultCode, data);
    }

    public static <T> R<T> of(IResultCode resultCode, String msg) {
        return new R<>(resultCode, msg);
    }

    public static <T> R<T> of(IResultCode resultCode, T data, String msg) {
        return new R<>(resultCode, data, msg);
    }

    /**
     * Convenience factory methods for common success and failure cases.
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

    public static <T> R<T> fail(IResultCode resultCode) {
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

    public static <T> R<T> fail(IResultCode resultCode, String msg) {
        return of(resultCode, msg);
    }
}
