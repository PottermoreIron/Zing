package com.pot.auth.domain.port.dto;

import com.pot.auth.domain.shared.valueobject.DeviceId;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 设备DTO（领域层）
 *
 * @author pot
 * @since 2025-12-14
 */
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

