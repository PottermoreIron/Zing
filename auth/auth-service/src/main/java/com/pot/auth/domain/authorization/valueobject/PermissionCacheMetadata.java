package com.pot.auth.domain.authorization.valueobject;

/**
 * 权限缓存元数据
 *
 * <p>
 * 封装权限版本号和摘要信息，用于Token中的权限验证
 *
 * <p>
 * 特性：
 * <ul>
 * <li>版本号：用于检测权限变更，实现Token实时失效</li>
 * <li>摘要：SHA-256散列，用于防篡改验证</li>
 * </ul>
 *
 * @param version 权限版本号
 * @param digest  权限摘要（SHA-256 Hex）
 * @author pot
 * @since 2025-12-14
 */
public record PermissionCacheMetadata(
        long version,
        String digest) {
    /**
     * 验证参数
     */
    public PermissionCacheMetadata {
        if (version < 0) {
            throw new IllegalArgumentException("权限版本号不能为负数");
        }
        if (digest == null || digest.isBlank()) {
            throw new IllegalArgumentException("权限摘要不能为空");
        }
    }

    /**
     * 创建空权限的元数据
     *
     * @param version 版本号
     * @return 元数据
     */
    public static PermissionCacheMetadata empty(long version) {
        return new PermissionCacheMetadata(version, "empty");
    }
}
