package com.pot.zing.framework.starter.ratelimit.service.impl;

import com.pot.zing.framework.starter.ratelimit.annotation.RateLimit;
import com.pot.zing.framework.starter.ratelimit.enums.RateLimitMethodEnum;
import com.pot.zing.framework.starter.ratelimit.service.RateLimitKeyProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Key provider that appends the real client IP address.
 *
 * <p>
 * Only {@code X-Real-IP} (injected by the reverse proxy) and the TCP remote
 * address are trusted. User-controllable headers such as
 * {@code X-Forwarded-For}
 * are intentionally ignored to prevent rate-limit bypass via header spoofing.
 */
@Slf4j
public class IpBasedRateLimitKeyProvider implements RateLimitKeyProvider {

    /**
     * Header name set by the reverse proxy (nginx:
     * {@code proxy_set_header X-Real-IP $remote_addr}).
     * This value cannot be forged by the client because the gateway overwrites it.
     */
    private static final String REAL_IP_HEADER = "X-Real-IP";

    @Override
    public String generateKey(String baseKey, ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String clientIp = getClientIp();
        return clientIp != null ? baseKey + ":ip:" + clientIp : baseKey;
    }

    @Override
    public RateLimitMethodEnum getSupportedType() {
        return RateLimitMethodEnum.IP_BASED;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    /**
     * Returns the trusted client IP address.
     *
     * <p>
     * Prefers {@code X-Real-IP} set by the reverse proxy.
     * Falls back to the TCP remote address when the header is absent.
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes == null) {
                return null;
            }

            HttpServletRequest request = attributes.getRequest();

            String realIp = request.getHeader(REAL_IP_HEADER);
            if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp)) {
                return realIp.trim();
            }

            return request.getRemoteAddr();
        } catch (Exception e) {
            log.warn("[RateLimit] Failed to resolve client IP address", e);
            return null;
        }
    }
}
