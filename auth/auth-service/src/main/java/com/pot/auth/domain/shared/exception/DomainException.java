package com.pot.auth.domain.shared.exception;

import com.pot.auth.domain.shared.enums.AuthResultCode;
import lombok.Getter;

/**
 * 领域层基础异常
 *
 * @author pot
 * @since 1.0.0
 */
@Getter
public class DomainException extends RuntimeException {

    private AuthResultCode resultCode;

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }

    public DomainException(AuthResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }

    public DomainException(AuthResultCode resultCode, Throwable cause) {
        super(resultCode.getMsg(), cause);
        this.resultCode = resultCode;
    }
}

