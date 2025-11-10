package com.pot.auth.domain.shared.valueobject;

import com.pot.auth.domain.shared.exception.InvalidIpAddressException;
import lombok.Builder;

import java.util.regex.Pattern;

/**
 * IP地址值对象 (Domain Primitive)
 *
 * <p>封装IP地址的验证和业务行为
 * <ul>
 *   <li>支持IPv4和IPv6</li>
 *   <li>提供异地登录检测</li>
 * </ul>
 *
 * @author pot
 * @since 1.0.0
 */
@Builder
public record IpAddress(String value) {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"
    );

    /**
     * 紧凑构造器 - 自动验证
     */
    public IpAddress {
        if (value == null || value.isBlank()) {
            throw new InvalidIpAddressException("IP地址不能为空");
        }

        String trimmed = value.trim();

        if (!isValidIpAddress(trimmed)) {
            throw new InvalidIpAddressException("IP地址格式不正确: " + value);
        }

        value = trimmed;
    }

    /**
     * 从字符串创建IP地址值对象
     */
    public static IpAddress of(String value) {
        return new IpAddress(value);
    }

    /**
     * 验证是否是有效的IP地址
     */
    private static boolean isValidIpAddress(String ip) {
        return IPV4_PATTERN.matcher(ip).matches()
                || IPV6_PATTERN.matcher(ip).matches();
    }

    /**
     * 是否是IPv4
     */
    public boolean isIPv4() {
        return IPV4_PATTERN.matcher(value).matches();
    }

    /**
     * 是否是IPv6
     */
    public boolean isIPv6() {
        return IPV6_PATTERN.matcher(value).matches();
    }

    /**
     * 获取IP地址的地域信息（简化版，实际需要IP库）
     * 这里仅做示例，实际应使用专业的IP地址库
     */
    public String getRegion() {
        // TODO: 集成IP地址库获取真实地域
        if (isPrivateIP()) {
            return "内网";
        }
        return "未知地域";
    }

    /**
     * 判断是否是私有IP
     */
    public boolean isPrivateIP() {
        if (!isIPv4()) {
            return false;
        }

        String[] parts = value.split("\\.");
        int firstOctet = Integer.parseInt(parts[0]);
        int secondOctet = Integer.parseInt(parts[1]);

        // 10.0.0.0 - 10.255.255.255
        if (firstOctet == 10) {
            return true;
        }

        // 172.16.0.0 - 172.31.255.255
        if (firstOctet == 172 && secondOctet >= 16 && secondOctet <= 31) {
            return true;
        }

        // 192.168.0.0 - 192.168.255.255
        if (firstOctet == 192 && secondOctet == 168) {
            return true;
        }

        // 127.0.0.0 - 127.255.255.255 (localhost)
        if (firstOctet == 127) {
            return true;
        }

        return false;
    }

    /**
     * 判断是否与另一个IP在同一地域（简化版）
     * 实际应使用IP地址库判断
     */
    public boolean isSameRegion(IpAddress other) {
        if (this.equals(other)) {
            return true;
        }

        // 如果都是私有IP，认为在同一地域
        if (this.isPrivateIP() && other.isPrivateIP()) {
            return true;
        }

        // TODO: 实际应使用IP地址库判断地域
        return false;
    }
}

