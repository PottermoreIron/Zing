package com.pot.auth.domain.authentication.valueobject;

import com.pot.auth.domain.shared.valueobject.TokenId;
import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;

import java.util.Map;
import java.util.Set;

public record JwtToken(
        TokenId tokenId, // JTI - unique token identifier
        UserId userId, // user ID
        UserDomain userDomain, // user domain
        String nickname, // display name
        Set<String> authorities, // permission list
        Long issuedAt, // issued at (Unix timestamp)
        Long expiresAt, // expires at (Unix timestamp)
        String rawToken, // raw token string
        Map<String, Object> claimsMap // complete claims map for accessing custom fields
) {

        public JwtToken {
        if (tokenId == null) {
            throw new IllegalArgumentException("TokenId must not be blank");
        }
        if (userId == null) {
            throw new IllegalArgumentException("UserId must not be null");
        }
        if (userDomain == null) {
            throw new IllegalArgumentException("UserDomain must not be null");
        }
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("Nickname must not be blank");
        }
        if (authorities == null) {
            authorities = Set.of();
        }
        if (issuedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("Timestamps must not be null");
        }
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Token string must not be blank");
        }
        if (claimsMap == null) {
            claimsMap = Map.of();
        }
    }

        @SuppressWarnings("unchecked")
    public <T> T getClaim(String key, Class<T> clazz) {
        Object value = claimsMap.get(key);
        if (value == null) {
            return null;
        }

        try {
            if (clazz == Long.class && value instanceof Number) {
                return (T) Long.valueOf(((Number) value).longValue());
            }
            if (clazz == Integer.class && value instanceof Number) {
                return (T) Integer.valueOf(((Number) value).intValue());
            }

            return clazz.cast(value);
        } catch (ClassCastException e) {
            return null;
        }
    }

        public boolean isExpired() {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        return currentTimestamp >= expiresAt;
    }

        public boolean isExpiringSoon(long thresholdSeconds) {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        long remainingSeconds = expiresAt - currentTimestamp;
        return remainingSeconds <= thresholdSeconds;
    }

        public long getRemainingSeconds() {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        long remaining = expiresAt - currentTimestamp;
        return Math.max(0, remaining);
    }

        public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }

        public boolean hasAnyAuthority(Set<String> requiredAuthorities) {
        return requiredAuthorities.stream().anyMatch(authorities::contains);
    }

        public boolean hasAllAuthorities(Set<String> requiredAuthorities) {
        return authorities.containsAll(requiredAuthorities);
    }
}
