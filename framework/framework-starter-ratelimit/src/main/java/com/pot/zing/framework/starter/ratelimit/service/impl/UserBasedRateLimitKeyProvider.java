package com.pot.zing.framework.starter.ratelimit.service.impl;

import com.pot.zing.framework.starter.ratelimit.annotation.RateLimit;
import com.pot.zing.framework.starter.ratelimit.enums.RateLimitMethodEnum;
import com.pot.zing.framework.starter.ratelimit.service.RateLimitKeyProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Key provider that appends a user identifier.
 */
@Slf4j
public class UserBasedRateLimitKeyProvider implements RateLimitKeyProvider {

    /**
     * User ID request header.
     */
    private static final String USER_ID_HEADER = "X-User-Id";

    /**
     * User ID request parameter.
     */
    private static final String USER_ID_PARAM = "userId";

    /**
     * Fallback marker for anonymous users.
     */
    private static final String ANONYMOUS_USER = "anonymous";

    /**
     * Key segment separator.
     */
    private static final String KEY_SEPARATOR = ":";

    @Override
    public String generateKey(String baseKey, ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String userId = extractUserId();
        return buildRateLimitKey(baseKey, userId);
    }

    @Override
    public RateLimitMethodEnum getSupportedType() {
        return RateLimitMethodEnum.USER_BASED;
    }

    /**
     * Extracts the user identifier using header, parameter, then anonymous
     * fallback.
     */
    protected String extractUserId() {
        return Optional.ofNullable(getCurrentRequest())
                .map(this::getUserIdFromRequest)
                .filter(StringUtils::hasText)
                .orElseGet(() -> {
                    log.debug("Failed to resolve user ID, falling back to anonymous key");
                    return ANONYMOUS_USER;
                });
    }

    /**
     * Returns the current HTTP request when available.
     */
    protected HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.warn("Failed to retrieve current request: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the user identifier from the current request.
     */
    protected String getUserIdFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String userId = request.getHeader(USER_ID_HEADER);
        if (StringUtils.hasText(userId)) {
            return userId;
        }

        userId = request.getParameter(USER_ID_PARAM);
        return StringUtils.hasText(userId) ? userId : null;
    }

    /**
     * Appends the user segment to the base key.
     */
    protected String buildRateLimitKey(String baseKey, String userId) {
        return baseKey + KEY_SEPARATOR + "user" + KEY_SEPARATOR + userId;
    }
}