package com.pot.zing.framework.starter.touch.exception;

/**
 * @author: Pot
 * @created: 2025/10/19 15:44
 * @description: 触达异常
 */
public class TouchException extends RuntimeException {

    public TouchException() {
        super();
    }

    public TouchException(String message) {
        super(message);
    }

    public TouchException(String message, Throwable cause) {
        super(message, cause);
    }

    public TouchException(Throwable cause) {
        super(cause);
    }
}
