package com.pot.auth.domain.context;

import com.pot.auth.domain.shared.valueobject.DeviceInfo;
import com.pot.auth.domain.shared.valueobject.IpAddress;
import com.pot.auth.interfaces.dto.register.RegisterRequest;
import lombok.Builder;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 注册上下文
 *
 * <p>
 * 封装注册流程中所需的所有上下文信息，包括：
 * <ul>
 * <li>注册请求（多态）</li>
 * <li>IP地址</li>
 * <li>设备信息</li>
 * <li>邀请码（可选）</li>
 * <li>扩展属性（用于未来扩展）</li>
 * </ul>
 *
 * <p>
 * 使用 Record 实现不可变性，Builder 模式提供灵活构建
 *
 * @author pot
 * @since 2025-11-29
 */
@Builder
public record RegistrationContext(
        // 必需字段
        @NonNull
        RegisterRequest request,
        @NonNull
        IpAddress ipAddress,
        @NonNull
        DeviceInfo deviceInfo,

        // 可选字段
        String invitationCode,
        String referralCode,
        String sessionId,
        Map<String, Object> extraAttributes) {
    /**
     * 规范化构造器（Canonical Constructor）
     *
     * <p>
     * 在构造时进行参数校验和默认值设置
     */
    public RegistrationContext {
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
    public static RegistrationContext of(
            RegisterRequest request,
            String ipAddress,
            String userAgent) {
        return RegistrationContext.builder()
                .request(request)
                .ipAddress(IpAddress.of(ipAddress))
                .deviceInfo(DeviceInfo.fromUserAgent(userAgent != null ? userAgent : "Unknown"))
                .build();
    }

    /**
     * 添加扩展属性（返回新实例，保持不可变性）
     */
    public RegistrationContext withExtraAttribute(String key, Object value) {
        Map<String, Object> newAttributes = new HashMap<>(this.extraAttributes);
        newAttributes.put(key, value);
        return new RegistrationContext(
                request, ipAddress, deviceInfo,
                invitationCode, referralCode, sessionId,
                newAttributes);
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

    /**
     * 是否有邀请码
     */
    public boolean hasInvitationCode() {
        return invitationCode != null && !invitationCode.isBlank();
    }

    /**
     * 是否有推荐码
     */
    public boolean hasReferralCode() {
        return referralCode != null && !referralCode.isBlank();
    }
}
