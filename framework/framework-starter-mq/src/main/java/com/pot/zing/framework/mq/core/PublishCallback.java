package com.pot.zing.framework.mq.core;

/**
 * 发布确认回调接口
 *
 * @author Copilot
 * @since 2026-01-05
 */
public interface PublishCallback {

    /**
     * 发布成功回调
     */
    void onSuccess();

    /**
     * 发布失败回调
     *
     * @param cause 失败原因
     */
    void onFailure(Throwable cause);
}
