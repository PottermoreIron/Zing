package com.pot.auth.domain.shared.exception;

/**
 * 无效邮箱异常
 *
 * @author pot
 * @since 2025-12-14
 */
public class InvalidEmailException extends DomainException {

    public InvalidEmailException(String message) {
        super(message);
    }
}

