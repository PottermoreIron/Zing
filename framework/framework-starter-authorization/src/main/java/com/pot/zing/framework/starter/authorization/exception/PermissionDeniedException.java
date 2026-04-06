package com.pot.zing.framework.starter.authorization.exception;

/**
 * Exception thrown when permission evaluation denies access.
 */
public class PermissionDeniedException extends RuntimeException {

    public PermissionDeniedException(String message) {
        super(message);
    }

    public PermissionDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}