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
 * @author: Pot
 * @created: 2025/10/18 22:06
 * @description: 自定义Ip限流Key提供者
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
     * 获取客户端真实IP
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }

            HttpServletRequest request = attributes.getRequest();

            for (String header : IP_HEADER_CANDIDATES) {
                String ip = request.getHeader(header);
                if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    // 多次反向代理后会有多个IP值，第一个IP才是真实IP
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
