package com.pot.zing.framework.mq.core;

/**
 * Callback for publish confirmations.
 *
 * @author Copilot
 * @since 2026-01-05
 */
public interface PublishCallback {

    /**
     * Invoked when the publish succeeds.
     */
    void onSuccess();

    /**
     * Invoked when the publish fails.
     */
    void onFailure(Throwable cause);
}
