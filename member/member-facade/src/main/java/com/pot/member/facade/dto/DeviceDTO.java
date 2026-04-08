package com.pot.member.facade.dto;

import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;

@Builder
public record DeviceDTO(
        Long deviceId,
        Long memberId,
        String deviceToken,
        String deviceType,
        String deviceName,
        String osType,
        String osVersion,
        String appVersion,
        String lastLoginIp,
        Long lastLoginAt,
        String refreshToken) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
