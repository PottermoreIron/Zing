package com.pot.auth.domain.authorization.constant;

/**
 * Cache key segments and builders used by auth-service.
 */
public final class CacheKeyConstants {

    public static final String SEP = ":";

    public static final String AUTH_PREFIX = "auth";

    public static final String TOKEN = "token";

    public static final String REFRESH = "refresh";

    public static final String BLACKLIST = "blacklist";

    public static final String SESSION = "session";

    public static final String SESSIONS = "sessions";

    public static final String RATE_LIMIT = "ratelimit";

    public static final String PERMISSION = "perms";

    public static final String PERMISSION_VERSION = "perm:version";

    public static final String PERMISSION_DIGEST = "perm:digest";

    public static final String PERMISSION_BLOOM = "perms:bloom";

    private CacheKeyConstants() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * Builds an auth cache key from the provided segments.
     */
    public static String buildKey(String... parts) {
        if (parts == null || parts.length == 0) {
            return AUTH_PREFIX;
        }
        return AUTH_PREFIX + SEP + String.join(SEP, parts);
    }

    /**
     * Builds the refresh token cache key for use with CachePort.
     */
    public static String buildRefreshKey(String tokenId) {
        return REFRESH + SEP + tokenId;
    }

    /**
     * Builds the blacklist cache key for use with CachePort.
     */
    public static String buildBlacklistKey(String tokenId) {
        return BLACKLIST + SEP + tokenId;
    }

    /**
     * Builds the permission cache key for a user.
     */
    public static String buildPermissionKey(String userDomain, String userId) {
        return buildKey(PERMISSION, userDomain, userId);
    }

    /**
     * Builds the permission version key for a user.
     */
    public static String buildPermissionVersionKey(String userDomain, String userId) {
        return buildKey(PERMISSION_VERSION, userDomain, userId);
    }

    /**
     * Builds the permission digest key for a user.
     */
    public static String buildPermissionDigestKey(String userDomain, String userId) {
        return buildKey(PERMISSION_DIGEST, userDomain, userId);
    }

    /**
     * Builds the permission bloom filter key for a domain.
     */
    public static String buildPermissionBloomKey(String userDomain) {
        return buildKey(PERMISSION_BLOOM, userDomain);
    }

    /**
     * Builds the session index key for a user.
     * Returns the raw key segment (without the "auth:" prefix) for use with
     * CachePort,
     * which adds the prefix automatically via its adapter.
     */
    public static String buildSessionIndexKey(String userDomain, String userId) {
        return SESSIONS + SEP + userDomain + SEP + userId;
    }
}
