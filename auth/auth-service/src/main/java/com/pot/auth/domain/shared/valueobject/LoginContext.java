package com.pot.auth.domain.shared.valueobject;

import com.pot.zing.framework.common.util.TimeUtils;
import lombok.Builder;

import java.util.Objects;

@Builder
public record LoginContext(
        IpAddress ipAddress,
        DeviceInfo deviceInfo,
        Long timestamp
) {

        public LoginContext {
        Objects.requireNonNull(ipAddress, "IP address must not be null");
        Objects.requireNonNull(deviceInfo, "Device info must not be null");
        timestamp = timestamp != null ? timestamp : TimeUtils.currentTimestamp();
    }

        public static LoginContext of(IpAddress ipAddress, DeviceInfo deviceInfo) {
        return LoginContext.builder()
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .timestamp(TimeUtils.currentTimestamp())
                .build();
    }

        public boolean isSameRegion(LoginContext other) {
        return this.ipAddress.isSameRegion(other.ipAddress);
    }

        public boolean isSameDeviceType(LoginContext other) {
        return this.deviceInfo.deviceType().equals(other.deviceInfo.deviceType());
    }

        public String getFormattedTimestamp() {
        return java.time.Instant.ofEpochSecond(timestamp).toString();
    }
}

