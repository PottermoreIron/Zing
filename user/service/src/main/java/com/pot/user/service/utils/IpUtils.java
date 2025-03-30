package com.pot.user.service.utils;

import com.pot.common.utils.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author: Pot
 * @created: 2025/3/30 20:47
 * @description: Ip工具类
 */
public class IpUtils {
    private static final List<String> IP_HEADER_CANDIDATES = Arrays.asList(
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
            "REMOTE_ADDR",
            "X-Real-IP"
    );

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return IP_HEADER_CANDIDATES.stream()
                .map(request::getHeader)
                .filter(ValidationUtils::isValidIpV4)
                .findFirst()
                .flatMap(IpUtils::parseFirstValidIp)
                .orElseGet(request::getRemoteAddr);
    }

    private static Optional<String> parseFirstValidIp(String ipString) {
        return Arrays.stream(ipString.split(","))
                .map(String::trim)
                .filter(ValidationUtils::isValidIpV4)
                .findFirst();
    }
}
