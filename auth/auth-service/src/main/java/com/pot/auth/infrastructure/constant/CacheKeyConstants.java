package com.pot.auth.infrastructure.constant;

/**
 * 缓存Key常量
 *
 * <p>定义auth服务所有缓存key的前缀和模板，统一管理便于维护
 *
 * @author pot
 * @since 1.0.0
 */
public final class CacheKeyConstants {

    private CacheKeyConstants() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

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
}

