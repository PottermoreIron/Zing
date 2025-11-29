package com.pot.auth.domain.context;

import com.pot.auth.domain.shared.valueobject.DeviceInfo;
import com.pot.auth.domain.shared.valueobject.IpAddress;
import com.pot.auth.interfaces.dto.onestop.OneStopAuthRequest;
import lombok.Builder;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 一键认证上下文
 *
 * <p>
 * 封装一键认证流程中所需的所有上下文信息
 *
 * <p>
 * 使用 Record 实现不可变性，Builder 模式提供灵活构建
 *
 * @author pot
 * @since 2025-11-29
 */
@Builder
public record OneStopAuthContext(
        // 必需字段
        @NonNull OneStopAuthRequest request,
        @NonNull IpAddress ipAddress,
        @NonNull DeviceInfo deviceInfo,

        // 可选字段
        String sessionId,
        Map<String, Object> extraAttributes) {

    /**
     * 规范化构造器（Canonical Constructor）
     *
     * <p>
     * 在构造时进行参数校验和默认值设置
     */
    public OneStopAuthContext {
        // 确保不可变集合的安全性
        if (extraAttributes == null) {
            extraAttributes = Map.of();
        } else {
            // 创建不可变副本
            extraAttributes = Map.copyOf(extraAttributes);
        }
    }

    /**
     * 便捷工厂方法 - 从基础参数创建
     */
    public static OneStopAuthContext of(
            OneStopAuthRequest request,
            String ipAddress,
            String userAgent) {
        return OneStopAuthContext.builder()
                .request(request)
                .ipAddress(IpAddress.of(ipAddress))
                .deviceInfo(DeviceInfo.fromUserAgent(userAgent != null ? userAgent : "Unknown"))
                .build();
    }

    /**
     * 添加扩展属性（返回新实例，保持不可变性）
     */
    public OneStopAuthContext withExtraAttribute(String key, Object value) {
        Map<String, Object> newAttributes = new HashMap<>(this.extraAttributes);
        newAttributes.put(key, value);
        return new OneStopAuthContext(request, ipAddress, deviceInfo, sessionId, newAttributes);
    }

    /**
     * 获取扩展属性
     */
    public Object getExtraAttribute(String key) {
        return extraAttributes.get(key);
    }

    /**
     * 获取扩展属性（带默认值）
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtraAttribute(String key, T defaultValue) {
        return (T) extraAttributes.getOrDefault(key, defaultValue);
    }
}
