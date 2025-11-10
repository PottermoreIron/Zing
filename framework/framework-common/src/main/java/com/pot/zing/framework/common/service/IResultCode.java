package com.pot.zing.framework.common.service;

/**
 * @author: Pot
 * @created: 2025/11/10 19:54
 * @description: 错误码接口
 */
public interface IResultCode {
    /**
     * 获取错误码
     *
     * @return 返回错误码
     */
    String getCode();

    /**
     * 获取错误信息
     *
     * @return 返回错误信息
     */
    String getMsg();

    /**
     * 是否成功
     *
     * @return 返回是否成功
     */
    boolean isSuccess();
}
