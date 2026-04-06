package com.pot.member.facade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long deviceId;
    private Long memberId;
    private String deviceToken;
    private String deviceType;
    private String deviceName;
    private String osType;
    private String osVersion;
    private String appVersion;
    private String lastLoginIp;
    private Long lastLoginAt;
    private String refreshToken;
}
