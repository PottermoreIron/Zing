package com.pot.auth.domain.shared.exception;

public class WeakPasswordException extends DomainException {
    public WeakPasswordException(String message) {
        super(message);
    }
}

