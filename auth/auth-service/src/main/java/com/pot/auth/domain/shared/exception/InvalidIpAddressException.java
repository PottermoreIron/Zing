package com.pot.auth.domain.shared.exception;

import com.pot.auth.domain.shared.enums.AuthResultCode;

public class InvalidIpAddressException extends DomainException {
    public InvalidIpAddressException(String message) {
        super(AuthResultCode.INVALID_IP_ADDRESS, message);
    }
}
