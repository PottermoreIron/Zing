package com.pot.user.service.ratelimit.impl;

import com.pot.user.service.annotations.ratelimit.RateLimit;
import com.pot.user.service.enums.ratelimit.RateLimitMethodEnum;
import com.pot.user.service.ratelimit.RateLimitKeyProvider;
import com.pot.user.service.utils.HttpUtils;
import com.pot.user.service.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author: Pot
 * @created: 2025/3/30 21:02
 * @description: 基于IP的限流key提供者
 */
public class IpBasedRateLimitKeyProvider implements RateLimitKeyProvider {
    @Override
    public String getKey(String baseKey, ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        HttpServletRequest request = HttpUtils.getRequest();
        if (request == null) {
            return baseKey;
        }

        String ip = IpUtils.getClientIp(request);
        return baseKey + ":" + ip;
    }

    @Override
    public RateLimitMethodEnum getType() {
        return RateLimitMethodEnum.IP_BASED;
    }
}
