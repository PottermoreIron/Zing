package com.pot.auth.domain.authorization.valueobject;

public record PermissionVersion(long value) {

        public PermissionVersion {
        if (value < 0) {
            throw new IllegalArgumentException("权限版本号不能为负数");
        }
    }

        public static PermissionVersion initial() {
        return new PermissionVersion(1L);
    }

        public PermissionVersion increment() {
        return new PermissionVersion(value + 1);
    }

        public boolean isOlderThan(PermissionVersion other) {
        return this.value < other.value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
