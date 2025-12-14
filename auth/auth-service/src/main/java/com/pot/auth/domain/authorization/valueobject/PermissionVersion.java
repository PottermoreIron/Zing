package com.pot.auth.domain.authorization.valueobject;

/**
 * 权限版本号值对象
 *
 * <p>
 * 权限版本号用于实现Token的实时失效机制：
 * <ul>
 * <li>用户登录时，权限版本号写入Token</li>
 * <li>权限变更时，版本号递增</li>
 * <li>鉴权时比对Token中的版本号，不匹配则拒绝访问</li>
 * </ul>
 *
 * @param value 版本号
 * @author pot
 * @since 2025-12-14
 */
public record PermissionVersion(long value) {

    /**
     * 验证参数
     */
    public PermissionVersion {
        if (value < 0) {
            throw new IllegalArgumentException("权限版本号不能为负数");
        }
    }

    /**
     * 初始版本号
     */
    public static PermissionVersion initial() {
        return new PermissionVersion(1L);
    }

    /**
     * 递增版本号
     *
     * @return 新版本号
     */
    public PermissionVersion increment() {
        return new PermissionVersion(value + 1);
    }

    /**
     * 比较版本号
     *
     * @param other 另一个版本号
     * @return true if当前版本号小于other
     */
    public boolean isOlderThan(PermissionVersion other) {
        return this.value < other.value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
