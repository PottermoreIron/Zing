package com.pot.auth.domain.authorization.valueobject;

public record PermissionCacheMetadata(
        long version,
        String digest) {
        public PermissionCacheMetadata {
        if (version < 0) {
            throw new IllegalArgumentException("Permission version must not be negative");
        }
        if (digest == null || digest.isBlank()) {
            throw new IllegalArgumentException("Permission digest must not be blank");
        }
    }

        public static PermissionCacheMetadata empty(long version) {
        return new PermissionCacheMetadata(version, "empty");
    }
}
