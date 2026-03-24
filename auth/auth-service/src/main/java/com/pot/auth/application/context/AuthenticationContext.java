package com.pot.auth.application.context;

import com.pot.auth.domain.shared.valueobject.DeviceInfo;
import com.pot.auth.domain.shared.valueobject.IpAddress;
import com.pot.auth.interfaces.dto.auth.LoginRequest;
import lombok.Builder;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

@Builder
public record AuthenticationContext(
        @NonNull LoginRequest request,
        @NonNull IpAddress ipAddress,
        @NonNull DeviceInfo deviceInfo,
        String sessionId,
        Map<String, Object> extraAttributes) {

    public AuthenticationContext {
        if (extraAttributes == null) {
            extraAttributes = Map.of();
        } else {
            extraAttributes = Map.copyOf(extraAttributes);
        }
    }

    public static AuthenticationContext of(
            LoginRequest request,
            String ipAddress,
            String userAgent) {
        return AuthenticationContext.builder()
                .request(request)
                .ipAddress(IpAddress.of(ipAddress))
                .deviceInfo(DeviceInfo.fromUserAgent(userAgent != null ? userAgent : "Unknown"))
                .build();
    }

    public AuthenticationContext withExtraAttribute(String key, Object value) {
        Map<String, Object> newAttributes = new HashMap<>(this.extraAttributes);
        newAttributes.put(key, value);
        return new AuthenticationContext(request, ipAddress, deviceInfo, sessionId, newAttributes);
    }

    public Object getExtraAttribute(String key) {
        return extraAttributes.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getExtraAttribute(String key, T defaultValue) {
        return (T) extraAttributes.getOrDefault(key, defaultValue);
    }
}