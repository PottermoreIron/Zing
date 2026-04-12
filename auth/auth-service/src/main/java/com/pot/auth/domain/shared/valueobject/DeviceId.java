package com.pot.auth.domain.shared.valueobject;

public record DeviceId(Long value) {

    public DeviceId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("DeviceId must be a positive integer");
        }
    }

    public static DeviceId of(Long value) {
        return new DeviceId(value);
    }

    public static DeviceId of(String value) {
        try {
            return new DeviceId(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid DeviceId format: " + value, e);
        }
    }
}

