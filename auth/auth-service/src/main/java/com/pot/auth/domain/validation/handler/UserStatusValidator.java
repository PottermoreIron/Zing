package com.pot.auth.domain.validation.handler;

import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AccountStatus;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.exception.DomainException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserStatusValidator {

    public static void validate(UserDTO user) {
        if (user == null) {
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }

        AccountStatus status = AccountStatus.fromCode(user.status());

        log.debug("[StatusCheck] userId={}, rawStatus={}, parsedStatus={}, canLogin={}",
                user.userId(), user.status(), status, status.isLoginAllowed());

        if (!status.isLoginAllowed()) {
            log.warn("[StatusCheck] Login denied — userId={}, status={}, reason={}",
                    user.userId(), status, status.getLoginDeniedReason());
            throw new DomainException(resolveStatusCode(status));
        }

        if (status == AccountStatus.UNKNOWN) {
            log.warn("[StatusCheck] Unknown status detected, login allowed — userId={}, rawStatus={}",
                    user.userId(), user.status());
        }
    }

    public static void validateStrict(UserDTO user) {
        if (user == null) {
            throw new DomainException(AuthResultCode.USER_NOT_FOUND);
        }

        AccountStatus status = AccountStatus.fromCode(user.status());

        if (status == AccountStatus.UNKNOWN) {
            log.error("[StatusCheck(strict)] Unknown status detected, login denied — userId={}, rawStatus={}",
                    user.userId(), user.status());
            throw new DomainException(AuthResultCode.ACCOUNT_STATUS_ABNORMAL);
        }

        if (!status.isLoginAllowed()) {
            log.warn("[StatusCheck(strict)] Login denied — userId={}, status={}, reason={}",
                    user.userId(), status, status.getLoginDeniedReason());
            throw new DomainException(resolveStatusCode(status));
        }
    }

    private static AuthResultCode resolveStatusCode(AccountStatus status) {
        return switch (status) {
            case LOCKED -> AuthResultCode.ACCOUNT_LOCKED;
            case INACTIVE -> AuthResultCode.ACCOUNT_DISABLED;
            default -> AuthResultCode.ACCOUNT_STATUS_ABNORMAL;
        };
    }
}
