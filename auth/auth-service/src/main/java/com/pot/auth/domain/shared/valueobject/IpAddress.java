package com.pot.auth.domain.shared.valueobject;

import com.pot.auth.domain.shared.exception.InvalidIpAddressException;
import lombok.Builder;

import java.util.regex.Pattern;

/**
 * Domain primitive for validated IPv4 and IPv6 addresses.
 *
 * @author pot
 * @since 2025-12-14
 */
@Builder
public record IpAddress(String value) {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");

    /**
     * Compact constructor with automatic validation.
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
     * Creates an IP address value object from a raw string.
     */
    public static IpAddress of(String value) {
        return new IpAddress(value);
    }

    /**
     * Validates whether the input is a supported IP address.
     */
    private static boolean isValidIpAddress(String ip) {
        return IPV4_PATTERN.matcher(ip).matches()
                || IPV6_PATTERN.matcher(ip).matches();
    }

    /** Returns whether the value is IPv4. */
    public boolean isIPv4() {
        return IPV4_PATTERN.matcher(value).matches();
    }

    /** Returns whether the value is IPv6. */
    public boolean isIPv6() {
        return IPV6_PATTERN.matcher(value).matches();
    }

    /**
     * Returns a coarse region label based on the current private/public heuristic.
     */
    public String getRegion() {
        if (isPrivateIP()) {
            return "内网";
        }
        return "未知地域";
    }

    /** Returns whether the address is in a private IPv4 range. */
    public boolean isPrivateIP() {
        if (!isIPv4()) {
            return false;
        }

        String[] parts = value.split("\\.");
        int firstOctet = Integer.parseInt(parts[0]);
        int secondOctet = Integer.parseInt(parts[1]);

        if (firstOctet == 10) {
            return true;
        }

        if (firstOctet == 172 && secondOctet >= 16 && secondOctet <= 31) {
            return true;
        }

        if (firstOctet == 192 && secondOctet == 168) {
            return true;
        }

        if (firstOctet == 127) {
            return true;
        }

        return false;
    }

    /**
     * Compares two addresses with the current heuristic region check.
     */
    public boolean isSameRegion(IpAddress other) {
        if (this.equals(other)) {
            return true;
        }

        if (this.isPrivateIP() && other.isPrivateIP()) {
            return true;
        }

        return false;
    }
}
