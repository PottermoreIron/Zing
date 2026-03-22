package com.pot.member.service.domain.model.device;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 设备聚合根
 *
 * <p>
 * 记录会员登录的设备信息，支持设备管理（踢出、查询）。
 *
 * @author Pot
 * @since 2026-03-18
 */
@Getter
public class DeviceAggregate {

    private Long id;
    private Long memberId;
    private String deviceToken;
    private String deviceType;
    private String deviceName;
    private String osType;
    private String osVersion;
    private String appVersion;
    private String lastLoginIp;
    private LocalDateTime lastLoginAt;
    private String refreshToken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private DeviceAggregate() {
    }

    /**
     * 记录设备登录（新设备或更新已有设备）
     */
    public static DeviceAggregate create(Long memberId, String deviceToken,
            String deviceType, String deviceName,
            String osType, String osVersion,
            String appVersion, String lastLoginIp,
            String refreshToken) {
        DeviceAggregate device = new DeviceAggregate();
        device.memberId = memberId;
        device.deviceToken = deviceToken;
        device.deviceType = deviceType;
        device.deviceName = deviceName;
        device.osType = osType;
        device.osVersion = osVersion;
        device.appVersion = appVersion;
        device.lastLoginIp = lastLoginIp;
        device.refreshToken = refreshToken;
        device.lastLoginAt = LocalDateTime.now();
        device.createdAt = LocalDateTime.now();
        device.updatedAt = LocalDateTime.now();
        return device;
    }

    public static DeviceAggregate reconstitute(Long id, Long memberId, String deviceToken,
            String deviceType, String deviceName,
            String osType, String osVersion,
            String appVersion, String lastLoginIp,
            LocalDateTime lastLoginAt, String refreshToken,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        DeviceAggregate device = new DeviceAggregate();
        device.id = id;
        device.memberId = memberId;
        device.deviceToken = deviceToken;
        device.deviceType = deviceType;
        device.deviceName = deviceName;
        device.osType = osType;
        device.osVersion = osVersion;
        device.appVersion = appVersion;
        device.lastLoginIp = lastLoginIp;
        device.lastLoginAt = lastLoginAt;
        device.refreshToken = refreshToken;
        device.createdAt = createdAt;
        device.updatedAt = updatedAt;
        return device;
    }

    /**
     * 更新登录信息（设备再次登录时调用）
     */
    public void updateLogin(String lastLoginIp, String refreshToken) {
        this.lastLoginIp = lastLoginIp;
        this.refreshToken = refreshToken;
        this.lastLoginAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
