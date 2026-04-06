package com.pot.zing.framework.common.service;

/**
 * Contract for framework result codes.
 */
public interface IResultCode {
    /**
     * Returns the code value.
     */
    String getCode();

    /**
     * Returns the display message.
     */
    String getMsg();

    /**
     * Indicates whether the code represents success.
     */
    boolean isSuccess();
}
