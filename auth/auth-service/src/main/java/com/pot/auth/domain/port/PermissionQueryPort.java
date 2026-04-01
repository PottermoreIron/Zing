package com.pot.auth.domain.port;

import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;

import java.util.Set;

/**
 * 权限查询端口
 *
 * <p>
 * 为权限读取提供稳定抽象，调用方无需感知本地缓存、Redis 或其他查询细节。
 */
public interface PermissionQueryPort {

    /**
     * 查询用户当前缓存权限
     *
     * @param userId     用户ID
     * @param userDomain 用户域
     * @return 权限集合，不存在时返回空集合
     */
    Set<String> getCachedPermissions(UserId userId, UserDomain userDomain);
}