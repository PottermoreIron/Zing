package com.pot.auth.domain.shared.exception;

/**
 * 无效邮箱异常
 *
 * @author pot
 * @since 1.0.0
 */
public class InvalidEmailException extends DomainException {

    public InvalidEmailException(String message) {
        super(message);
    }
}

