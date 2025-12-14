package com.pot.auth.infrastructure.security.exception;

/**
 * 权限拒绝异常
 *
 * <p>
 * 用户缺少必要权限时抛出此异常
 *
 * @author pot
 * @since 2025-12-14
 */
public class PermissionDeniedException extends RuntimeException {

    public PermissionDeniedException(String message) {
        super(message);
    }

    public PermissionDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
