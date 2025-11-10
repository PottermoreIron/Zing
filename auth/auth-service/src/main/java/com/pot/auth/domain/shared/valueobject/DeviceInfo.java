package com.pot.auth.domain.shared.valueobject;

/**
 * 设备信息值对象
 *
 * <p>包含设备的详细信息，用于设备管理和安全审计
 *
 * @author yecao
 * @since 2025-11-10
 */
public record DeviceInfo(
        String deviceType,    // MOBILE, DESKTOP, TABLET, UNKNOWN
        String osName,        // iOS, Android, Windows, macOS, Linux
        String osVersion,     // 操作系统版本
        String browserName,   // Chrome, Safari, Firefox, Edge
        String browserVersion, // 浏览器版本
        String userAgent      // 完整的User-Agent
) {

    /**
     * 验证参数
     */
    public DeviceInfo {
        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalArgumentException("User-Agent不能为空");
        }
        // 如果解析字段为空，设置默认值
        deviceType = deviceType != null ? deviceType : "UNKNOWN";
        osName = osName != null ? osName : "Unknown";
        osVersion = osVersion != null ? osVersion : "Unknown";
        browserName = browserName != null ? browserName : "Unknown";
        browserVersion = browserVersion != null ? browserVersion : "Unknown";
    }

    /**
     * 从User-Agent创建（简化版本，实际应使用专业库解析）
     */
    public static DeviceInfo fromUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "Unknown";
        }

        // 简单解析（生产环境应使用 ua-parser 等专业库）
        String deviceType = detectDeviceType(userAgent);
        String osName = detectOsName(userAgent);
        String browserName = detectBrowserName(userAgent);

        return new DeviceInfo(
                deviceType,
                osName,
                "Unknown",
                browserName,
                "Unknown",
                userAgent
        );
    }

    /**
     * 检测设备类型
     */
    private static String detectDeviceType(String ua) {
        String uaLower = ua.toLowerCase();
        if (uaLower.contains("mobile") || uaLower.contains("android")) {
            return "MOBILE";
        } else if (uaLower.contains("tablet") || uaLower.contains("ipad")) {
            return "TABLET";
        } else if (uaLower.contains("windows") || uaLower.contains("macintosh") || uaLower.contains("linux")) {
            return "DESKTOP";
        }
        return "UNKNOWN";
    }

    /**
     * 检测操作系统
     */
    private static String detectOsName(String ua) {
        String uaLower = ua.toLowerCase();
        if (uaLower.contains("android")) return "Android";
        if (uaLower.contains("iphone") || uaLower.contains("ipad")) return "iOS";
        if (uaLower.contains("windows")) return "Windows";
        if (uaLower.contains("macintosh") || uaLower.contains("mac os")) return "macOS";
        if (uaLower.contains("linux")) return "Linux";
        return "Unknown";
    }

    /**
     * 检测浏览器
     */
    private static String detectBrowserName(String ua) {
        String uaLower = ua.toLowerCase();
        if (uaLower.contains("edg")) return "Edge";
        if (uaLower.contains("chrome")) return "Chrome";
        if (uaLower.contains("safari")) return "Safari";
        if (uaLower.contains("firefox")) return "Firefox";
        return "Unknown";
    }

    /**
     * 是否为移动设备
     */
    public boolean isMobile() {
        return "MOBILE".equals(deviceType);
    }

    /**
     * 是否为桌面设备
     */
    public boolean isDesktop() {
        return "DESKTOP".equals(deviceType);
    }
}

