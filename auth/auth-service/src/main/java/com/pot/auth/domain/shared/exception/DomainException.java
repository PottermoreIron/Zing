package com.pot.auth.domain.shared.exception;

import com.pot.auth.domain.shared.enums.AuthResultCode;
import lombok.Getter;

@Getter
public class DomainException extends RuntimeException {

    private final AuthResultCode resultCode;

    public DomainException(AuthResultCode resultCode) {
        super(resultCode.getMsg());
        this.resultCode = resultCode;
    }

    public DomainException(AuthResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public DomainException(AuthResultCode resultCode, Throwable cause) {
        super(resultCode.getMsg(), cause);
        this.resultCode = resultCode;
    }

    public int getHttpStatus() {
        return resultCode.getHttpStatus();
    }
}
