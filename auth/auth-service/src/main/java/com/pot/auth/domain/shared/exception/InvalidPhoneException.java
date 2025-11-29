package com.pot.auth.domain.shared.exception;

/**
 * 无效手机号异常
 *
 * @author pot
 * @since 2025-11-10
 */
public class InvalidPhoneException extends DomainException {
    public InvalidPhoneException(String message) {
        super(message);
    }
}

