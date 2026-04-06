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
 * Key provider that appends the client IP address.
 */
@Slf4j
public class IpBasedRateLimitKeyProvider implements RateLimitKeyProvider {

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

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
     * Resolves the best-effort client IP address.
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes == null) {
                return null;
            }

            HttpServletRequest request = attributes.getRequest();

            for (String header : IP_HEADER_CANDIDATES) {
                String ip = request.getHeader(header);
                if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    // X-Forwarded-For may contain multiple hops; the first value is the client IP.
                    int index = ip.indexOf(',');
                    return index != -1 ? ip.substring(0, index) : ip;
                }
            }

            return request.getRemoteAddr();
        } catch (Exception e) {
            log.warn("获取客户端IP失败", e);
            return null;
        }
    }
}
