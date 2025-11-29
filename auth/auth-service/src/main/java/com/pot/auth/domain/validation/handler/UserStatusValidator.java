package com.pot.auth.domain.validation.handler;

import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AccountStatus;
import com.pot.auth.domain.shared.exception.DomainException;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户状态校验器
 *
 * <p>
 * 校验用户账户状态是否允许登录
 * <p>
 * 设计特点：
 * <ul>
 * <li>独立的校验器，可在责任链中灵活配置</li>
 * <li>使用健壮的状态匹配策略，兼容不同User模块</li>
 * <li>提供清晰的错误提示</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
public class UserStatusValidator {

    /**
     * 校验用户状态
     *
     * @param user 用户信息
     * @throws DomainException 当用户状态不允许登录时抛出
     */
    public static void validate(UserDTO user) {
        if (user == null) {
            throw new DomainException("用户不存在");
        }

        // 解析用户状态（健壮处理，兼容不同User模块的状态值）
        AccountStatus status = AccountStatus.fromCode(user.status());

        log.debug("[状态校验] userId={}, rawStatus={}, parsedStatus={}, canLogin={}",
                user.userId(), user.status(), status, status.isLoginAllowed());

        // 检查是否允许登录
        if (!status.isLoginAllowed()) {
            String reason = status.getLoginDeniedReason();
            log.warn("[状态校验] 登录被拒绝: userId={}, status={}, reason={}",
                    user.userId(), status, reason);
            throw new DomainException(reason);
        }

        // 如果状态是UNKNOWN，记录警告但允许登录（宽松策略）
        if (status == AccountStatus.UNKNOWN) {
            log.warn("[状态校验] 检测到未知状态，但允许登录: userId={}, rawStatus={}",
                    user.userId(), user.status());
        }
    }

    /**
     * 校验用户状态（严格模式）
     *
     * <p>
     * 在严格模式下，UNKNOWN状态也会被拒绝
     *
     * @param user 用户信息
     * @throws DomainException 当用户状态不允许登录时抛出
     */
    public static void validateStrict(UserDTO user) {
        if (user == null) {
            throw new DomainException("用户不存在");
        }

        AccountStatus status = AccountStatus.fromCode(user.status());

        // 严格模式：UNKNOWN状态也拒绝
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
