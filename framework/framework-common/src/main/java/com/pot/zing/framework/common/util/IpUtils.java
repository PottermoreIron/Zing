package com.pot.zing.framework.common.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;

/**
 * Client IP extraction helpers.
 */
public class IpUtils {
    private static final String UNKNOWN = "unknown";
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
            "X-Real-IP");

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        return IP_HEADER_CANDIDATES.stream()
                .map(request::getHeader)
                .filter(ip -> ip != null && !ip.isBlank())
                .flatMap(ip -> Arrays.stream(ip.split(",\\s*")))
                .map(IpUtils::normalizeIp)
                .filter(ValidationUtils::isValidIpV4)
                .findFirst()
                .orElseGet(() -> normalizeIp(request.getRemoteAddr()));
    }

    /**
     * Normalizes loopback and IPv4-mapped addresses.
     */
    private static String normalizeIp(String ip) {
        if (ip == null || ip.isBlank())
            return "";
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip))
            return "127.0.0.1";
        return ip.startsWith("::ffff:") ? ip.substring(7) : ip.trim();
    }
}
