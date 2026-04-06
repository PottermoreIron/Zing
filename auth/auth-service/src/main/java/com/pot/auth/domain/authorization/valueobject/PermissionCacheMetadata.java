package com.pot.auth.domain.authorization.valueobject;

public record PermissionCacheMetadata(
        long version,
        String digest) {
        public PermissionCacheMetadata {
        if (version < 0) {
            throw new IllegalArgumentException("权限版本号不能为负数");
        }
        if (digest == null || digest.isBlank()) {
            throw new IllegalArgumentException("权限摘要不能为空");
        }
    }

        public static PermissionCacheMetadata empty(long version) {
        return new PermissionCacheMetadata(version, "empty");
    }
}
