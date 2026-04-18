package com.pot.zing.framework.starter.security.exception;

/**
 * Thrown when permission or role evaluation denies access.
 */
public class PermissionDeniedException extends RuntimeException {

    public PermissionDeniedException(String message) {
        super(message);
    }

    public PermissionDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
