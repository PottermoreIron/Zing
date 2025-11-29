package com.pot.auth.domain.shared.valueobject;

import com.pot.zing.framework.common.util.TimeUtils;
import lombok.Builder;

import java.util.Objects;

/**
 * 登录上下文值对象
 *
 * <p>封装登录时的上下文信息，用于安全审计和异常检测
 *
 * @author pot
 * @since 2025-11-10
 */
@Builder
public record LoginContext(
        IpAddress ipAddress,
        DeviceInfo deviceInfo,
        Long timestamp
) {

    /**
     * 验证参数
     */
    public LoginContext {
        Objects.requireNonNull(ipAddress, "IP地址不能为空");
        Objects.requireNonNull(deviceInfo, "设备信息不能为空");
        timestamp = timestamp != null ? timestamp : TimeUtils.currentTimestamp();
    }

    /**
     * 静态工厂方法
     */
    public static LoginContext of(IpAddress ipAddress, DeviceInfo deviceInfo) {
        return LoginContext.builder()
                .ipAddress(ipAddress)
                .deviceInfo(deviceInfo)
                .timestamp(TimeUtils.currentTimestamp())
                .build();
    }

    /**
     * 检查是否与另一个上下文来自相同地域
     */
    public boolean isSameRegion(LoginContext other) {
        return this.ipAddress.isSameRegion(other.ipAddress);
    }

    /**
     * 检查是否与另一个上下文来自相同设备类型
     */
    public boolean isSameDeviceType(LoginContext other) {
        return this.deviceInfo.deviceType().equals(other.deviceInfo.deviceType());
    }

    /**
     * 获取可读的时间戳
     */
    public String getFormattedTimestamp() {
        return java.time.Instant.ofEpochSecond(timestamp).toString();
    }
}

