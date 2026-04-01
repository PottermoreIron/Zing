package com.pot.auth.domain.authorization.valueobject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.*;

/**
 * 权限摘要值对象
 *
 * <p>
 * 使用MD5算法计算权限集合的散列值，用于：
 * <ul>
 * <li>防篡改：验证Token中的权限是否被修改</li>
 * <li>快速比对：无需完整比对权限集合即可判断权限是否变更</li>
 * </ul>
 *
 * @param value 摘要值（MD5 Hex）
 * @author pot
 * @since 2025-12-14
 */
public record PermissionDigest(String value) {

    /**
     * 验证参数
     */
    public PermissionDigest {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("权限摘要不能为空");
        }
        if (!value.matches("^[a-f0-9]{32}$")) {
            throw new IllegalArgumentException("权限摘要格式错误，必须为32位MD5 Hex字符串");
        }
    }

    /**
     * 从权限集合计算摘要
     *
     * @param permissions 权限集合
     * @return 摘要
     */
    public static PermissionDigest from(Set<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return empty();
        }

        // 排序后计算，保证相同权限集合生成相同摘要
        List<String> sortedPerms = new ArrayList<>(permissions);
        Collections.sort(sortedPerms);
        String permsStr = String.join(",", sortedPerms);

        return new PermissionDigest(md5Hex(permsStr));
    }

    /**
     * 空权限摘要
     *
     * @return 空摘要
     */
    public static PermissionDigest empty() {
        return new PermissionDigest(md5Hex("empty"));
    }

    /**
     * 验证权限集合是否匹配当前摘要
     *
     * @param permissions 权限集合
     * @return true if匹配
     */
    public boolean matches(Set<String> permissions) {
        PermissionDigest other = from(permissions);
        return this.value.equals(other.value);
    }

    /**
     * 获取摘要简短形式（前8位）
     *
     * @return 简短摘要
     */
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
