package com.pot.auth.domain.validation.handler;

import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AccountStatus;
import com.pot.auth.domain.shared.exception.DomainException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserStatusValidator {

        public static void validate(UserDTO user) {
        if (user == null) {
            throw new DomainException("用户不存在");
        }

        AccountStatus status = AccountStatus.fromCode(user.status());

        log.debug("[状态校验] userId={}, rawStatus={}, parsedStatus={}, canLogin={}",
                user.userId(), user.status(), status, status.isLoginAllowed());

        if (!status.isLoginAllowed()) {
            String reason = status.getLoginDeniedReason();
            log.warn("[状态校验] 登录被拒绝: userId={}, status={}, reason={}",
                    user.userId(), status, reason);
            throw new DomainException(reason);
        }

        if (status == AccountStatus.UNKNOWN) {
            log.warn("[状态校验] 检测到未知状态，但允许登录: userId={}, rawStatus={}",
                    user.userId(), user.status());
        }
    }

        public static void validateStrict(UserDTO user) {
        if (user == null) {
            throw new DomainException("用户不存在");
        }

        AccountStatus status = AccountStatus.fromCode(user.status());

        if (status == AccountStatus.UNKNOWN) {
            log.error("[状态校验-严格] 检测到未知状态，拒绝登录: userId={}, rawStatus={}",
                    user.userId(), user.status());
            throw new DomainException("账户状态异常，请联系客服");
        }

        if (!status.isLoginAllowed()) {
            String reason = status.getLoginDeniedReason();
            log.warn("[状态校验-严格] 登录被拒绝: userId={}, status={}, reason={}",
                    user.userId(), status, reason);
            throw new DomainException(reason);
        }
    }
}
