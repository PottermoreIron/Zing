package com.pot.zing.framework.starter.id.exception;

/**
 * @author: Pot
 * @created: 2025/10/18 23:26
 * @description: 自定义分布式id生成器异常
 */
public class IdGenerationException extends RuntimeException {

    public IdGenerationException(String message) {
        super(message);
    }

    public IdGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
