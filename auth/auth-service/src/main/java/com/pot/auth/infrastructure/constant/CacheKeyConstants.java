package com.pot.auth.infrastructure.constant;

/**
 * 缓存Key常量
 *
 * <p>
 * 定义auth服务所有缓存key的前缀和模板，统一管理便于维护
 *
 * @author pot
 * @since 2025-12-14
 */
public final class CacheKeyConstants {

    /**
     * Auth服务缓存key前缀
     */
    public static final String AUTH_PREFIX = "auth";
    /**
     * Token相关
     */
    public static final String TOKEN = "token";
    /**
     * 验证码相关
     */
    public static final String CAPTCHA = "captcha";
    /**
     * 黑名单相关
     */
    public static final String BLACKLIST = "blacklist";
    /**
     * 用户会话相关
     */
    public static final String SESSION = "session";
    /**
     * 限流相关
     */
    public static final String RATE_LIMIT = "ratelimit";

    /**
     * 权限相关
     */
    public static final String PERMISSION = "perms";
    /**
     * 权限版本号
     */
    public static final String PERMISSION_VERSION = "perm:version";
    /**
     * 权限摘要
     */
    public static final String PERMISSION_DIGEST = "perm:digest";
    /**
     * 权限布隆过滤器
     */
    public static final String PERMISSION_BLOOM = "perms:bloom";

    private CacheKeyConstants() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 构建完整的缓存key
     *
     * @param parts key的各个部分
     * @return 完整的key，格式：auth:part1:part2:...
     */
    public static String buildKey(String... parts) {
        if (parts == null || parts.length == 0) {
            return AUTH_PREFIX;
        }
        return AUTH_PREFIX + ":" + String.join(":", parts);
    }

    /**
     * 构建权限缓存Key
     *
     * <p>
     * 格式：auth:perms:{userDomain}:{userId}
     * <p>
     * 示例：auth:perms:member:123456
     *
     * @param userDomain 用户域
     * @param userId     用户ID
     * @return Redis Key
     */
    public static String buildPermissionKey(String userDomain, String userId) {
        return buildKey(PERMISSION, userDomain, userId);
    }

    /**
     * 构建权限版本号Key
     *
     * <p>
     * 格式：auth:perm:version:{userDomain}:{userId}
     * <p>
     * 示例：auth:perm:version:member:123456
     *
     * @param userDomain 用户域
     * @param userId     用户ID
     * @return Redis Key
     */
    public static String buildPermissionVersionKey(String userDomain, String userId) {
        return buildKey(PERMISSION_VERSION, userDomain, userId);
    }

    /**
     * 构建权限摘要Key
     *
     * <p>
     * 格式：auth:perm:digest:{userDomain}:{userId}
     * <p>
     * 示例：auth:perm:digest:member:123456
     *
     * @param userDomain 用户域
     * @param userId     用户ID
     * @return Redis Key
     */
    public static String buildPermissionDigestKey(String userDomain, String userId) {
        return buildKey(PERMISSION_DIGEST, userDomain, userId);
    }

    /**
     * 构建布隆过滤器Key
     *
     * <p>
     * 格式：auth:perms:bloom:{userDomain}
     * <p>
     * 示例：auth:perms:bloom:member
     *
     * @param userDomain 用户域
     * @return Redis Key
     */
    public static String buildPermissionBloomKey(String userDomain) {
        return buildKey(PERMISSION_BLOOM, userDomain);
    }
}
