package com.pot.auth.infrastructure.exception;

/**
 * Signals infrastructure adapter failures in auth-service.
 */
public class AuthInfrastructureException extends RuntimeException {

    public AuthInfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}