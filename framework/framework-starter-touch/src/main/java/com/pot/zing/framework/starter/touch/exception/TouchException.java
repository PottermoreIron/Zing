package com.pot.zing.framework.starter.touch.exception;

/**
 * Exception thrown for touch delivery failures.
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
