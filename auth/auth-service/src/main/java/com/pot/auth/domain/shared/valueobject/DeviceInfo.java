package com.pot.auth.domain.shared.valueobject;

public record DeviceInfo(
        String deviceType,    // MOBILE, DESKTOP, TABLET, UNKNOWN
        String osName,        // iOS, Android, Windows, macOS, Linux
        String osVersion,     // OS version
        String browserName,   // Chrome, Safari, Firefox, Edge
        String browserVersion, // browser version
        String userAgent      // complete User-Agent string
) {

        public DeviceInfo {
        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalArgumentException("User-Agent must not be blank");
        }
        deviceType = deviceType != null ? deviceType : "UNKNOWN";
        osName = osName != null ? osName : "Unknown";
        osVersion = osVersion != null ? osVersion : "Unknown";
        browserName = browserName != null ? browserName : "Unknown";
        browserVersion = browserVersion != null ? browserVersion : "Unknown";
    }

        public static DeviceInfo fromUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "Unknown";
        }

        String deviceType = detectDeviceType(userAgent);
        String osName = detectOsName(userAgent);
        String browserName = detectBrowserName(userAgent);

        return new DeviceInfo(deviceType, osName, "Unknown", browserName, "Unknown", userAgent);
    }

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

        private static String detectOsName(String ua) {
        String uaLower = ua.toLowerCase();
        if (uaLower.contains("android")) return "Android";
        if (uaLower.contains("iphone") || uaLower.contains("ipad")) return "iOS";
        if (uaLower.contains("windows")) return "Windows";
        if (uaLower.contains("macintosh") || uaLower.contains("mac os")) return "macOS";
        if (uaLower.contains("linux")) return "Linux";
        return "Unknown";
    }

        private static String detectBrowserName(String ua) {
        String uaLower = ua.toLowerCase();
        if (uaLower.contains("edg")) return "Edge";
        if (uaLower.contains("chrome")) return "Chrome";
        if (uaLower.contains("safari")) return "Safari";
        if (uaLower.contains("firefox")) return "Firefox";
        return "Unknown";
    }

        public boolean isMobile() {
        return "MOBILE".equals(deviceType);
    }

        public boolean isDesktop() {
        return "DESKTOP".equals(deviceType);
    }
}

