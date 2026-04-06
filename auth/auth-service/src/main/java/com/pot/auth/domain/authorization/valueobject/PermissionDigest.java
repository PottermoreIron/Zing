package com.pot.auth.domain.authorization.valueobject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.*;

public record PermissionDigest(String value) {

        public PermissionDigest {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("权限摘要不能为空");
        }
        if (!value.matches("^[a-f0-9]{32}$")) {
            throw new IllegalArgumentException("权限摘要格式错误，必须为32位MD5 Hex字符串");
        }
    }

        public static PermissionDigest from(Set<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return empty();
        }

        List<String> sortedPerms = new ArrayList<>(permissions);
        Collections.sort(sortedPerms);
        String permsStr = String.join(",", sortedPerms);

        return new PermissionDigest(md5Hex(permsStr));
    }

        public static PermissionDigest empty() {
        return new PermissionDigest(md5Hex("empty"));
    }

        public boolean matches(Set<String> permissions) {
        PermissionDigest other = from(permissions);
        return this.value.equals(other.value);
    }

        public String shortValue() {
        return value.substring(0, Math.min(8, value.length()));
    }

    @Override
    public String toString() {
        return value;
    }

    private static String md5Hex(String content) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digestBytes = messageDigest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digestBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("MD5 algorithm is not available", exception);
        }
    }
}
