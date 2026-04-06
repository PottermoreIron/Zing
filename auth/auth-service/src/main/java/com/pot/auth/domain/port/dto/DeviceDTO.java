package com.pot.auth.domain.port.dto;

import com.pot.auth.domain.shared.valueobject.DeviceId;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DeviceDTO(
        DeviceId deviceId,
        String deviceType,
        String platform,
        String browser,
        String appVersion,
        boolean isActive,
        LocalDateTime lastUsedAt
) {
}

