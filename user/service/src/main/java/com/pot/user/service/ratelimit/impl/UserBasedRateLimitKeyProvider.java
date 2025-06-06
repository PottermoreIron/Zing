package com.pot.user.service.ratelimit.impl;

import com.pot.user.service.annotations.ratelimit.RateLimit;
import com.pot.user.service.enums.ratelimit.RateLimitMethodEnum;
import com.pot.user.service.ratelimit.RateLimitKeyProvider;
import com.pot.user.service.utils.HttpUtils;
import com.pot.user.service.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author: Pot
 * @created: 2025/3/30 21:04
 * @description: 基于用户ID的限流key提供者
 */
public class UserBasedRateLimitKeyProvider implements RateLimitKeyProvider {

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
        return JwtUtils.getUid(request).toString();
    }
}
