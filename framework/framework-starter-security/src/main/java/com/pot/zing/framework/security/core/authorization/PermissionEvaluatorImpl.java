package com.pot.zing.framework.security.core.authorization;

import com.pot.zing.framework.security.core.userdetails.SecurityUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 权限评估器实现
 * <p>
 * 用于@PreAuthorize等注解中的权限表达式评估
 * </p>
 *
 * @author Pot
 * @since 2025-01-24
 */
@Slf4j
@Component
public class PermissionEvaluatorImpl implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser)) {
            return false;
        }

        SecurityUser user = (SecurityUser) authentication.getPrincipal();
        String permissionStr = permission.toString();

        boolean hasPermission = user.hasPermission(permissionStr);
        log.debug("权限评估: userId={}, permission={}, result={}", user.getUserId(), permissionStr, hasPermission);

        return hasPermission;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser)) {
            return false;
        }

        SecurityUser user = (SecurityUser) authentication.getPrincipal();
        String permissionStr = permission.toString();

        boolean hasPermission = user.hasPermission(permissionStr);
        log.debug("权限评估: userId={}, targetType={}, targetId={}, permission={}, result={}",
                user.getUserId(), targetType, targetId, permissionStr, hasPermission);

        return hasPermission;
    }
}

