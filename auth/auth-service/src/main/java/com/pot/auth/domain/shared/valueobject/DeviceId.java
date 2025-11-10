package com.pot.auth.domain.shared.valueobject;

/**
 * 设备ID值对象
 *
 * @author pot
 * @since 1.0.0
 */
public record DeviceId(Long value) {

    public DeviceId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("DeviceId必须是正整数");
        }
    }

    public static DeviceId of(Long value) {
        return new DeviceId(value);
    }

    public static DeviceId of(String value) {
        try {
            return new DeviceId(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的DeviceId格式: " + value, e);
        }
    }
}

