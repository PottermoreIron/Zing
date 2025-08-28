package com.pot.common.ratelimit.impl;

import com.pot.common.annotations.ratelimit.RateLimit;
import com.pot.common.enums.ratelimit.RateLimitMethodEnum;
import com.pot.common.ratelimit.RateLimitKeyProvider;
import com.pot.common.utils.HttpUtils;
import com.pot.common.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/3/30 21:04
 * @description: 基于用户ID的限流key提供者
 */
@Component
@RequiredArgsConstructor
public class UserBasedRateLimitKeyProvider implements RateLimitKeyProvider {

    private final JwtUtils jwtUtils;

    @Override
    public String getKey(String baseKey, ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        HttpServletRequest request = HttpUtils.getRequest();
        if (request == null) {
            return baseKey;
        }

        String userId = getUserId(request);
        if (StringUtils.isEmpty(userId)) {
            return baseKey;
        }

        return baseKey + ":user:" + userId;
    }

    @Override
    public RateLimitMethodEnum getType() {
        return RateLimitMethodEnum.USER_BASED;
    }

    /**
     * 获取用户ID
     */
    private String getUserId(HttpServletRequest request) {
        return jwtUtils.getUid(request).toString();
    }
}
