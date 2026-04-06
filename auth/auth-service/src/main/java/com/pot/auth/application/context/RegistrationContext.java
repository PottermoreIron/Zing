package com.pot.auth.application.context;

import com.pot.auth.application.command.RegisterCommand;
import com.pot.auth.domain.shared.valueobject.DeviceInfo;
import com.pot.auth.domain.shared.valueobject.IpAddress;
import lombok.Builder;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

@Builder
public record RegistrationContext(
    @NonNull RegisterCommand request,
        @NonNull IpAddress ipAddress,
        @NonNull DeviceInfo deviceInfo,
        String invitationCode,
        String referralCode,
        String sessionId,
        Map<String, Object> extraAttributes) {

    public RegistrationContext {
        if (extraAttributes == null) {
            extraAttributes = Map.of();
        } else {
            extraAttributes = Map.copyOf(extraAttributes);
        }
    }

    public static RegistrationContext of(
            RegisterCommand request,
            String ipAddress,
            String userAgent) {
        return RegistrationContext.builder()
                .request(request)
                .ipAddress(IpAddress.of(ipAddress))
                .deviceInfo(DeviceInfo.fromUserAgent(userAgent != null ? userAgent : "Unknown"))
                .build();
    }

    public RegistrationContext withExtraAttribute(String key, Object value) {
        Map<String, Object> newAttributes = new HashMap<>(this.extraAttributes);
        newAttributes.put(key, value);
        return new RegistrationContext(
                request, ipAddress, deviceInfo,
                invitationCode, referralCode, sessionId,
                newAttributes);
    }

    public Object getExtraAttribute(String key) {
        return extraAttributes.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getExtraAttribute(String key, T defaultValue) {
        return (T) extraAttributes.getOrDefault(key, defaultValue);
    }

    public boolean hasInvitationCode() {
        return invitationCode != null && !invitationCode.isBlank();
    }

    public boolean hasReferralCode() {
        return referralCode != null && !referralCode.isBlank();
    }
}