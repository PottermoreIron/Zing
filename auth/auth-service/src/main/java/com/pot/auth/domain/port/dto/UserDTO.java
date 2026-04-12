package com.pot.auth.domain.port.dto;

import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
public record UserDTO(
                UserId userId, // using Long instead of UserId to simplify conversion
                UserDomain userDomain,
                String nickname,
                String email,
                String phone, // unified field name: phone
                String status,
                Set<String> permissions, // add permission set
                LocalDateTime emailVerifiedAt,
                LocalDateTime phoneVerifiedAt,
                LocalDateTime lastLoginAt,
                String lastLoginIp) {
}
