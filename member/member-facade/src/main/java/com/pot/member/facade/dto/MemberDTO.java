package com.pot.member.facade.dto;

import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * RPC DTO for core member data shared across services.
 *
 * @author Pot
 * @since 2026-03-18
 */
@Builder
public record MemberDTO(
        Long memberId,
        String nickname,
        String email,
        String phone,
        String avatarUrl,
        String status,
        Set<String> roleCodes,
        Set<String> permissionCodes,
        Long gmtCreatedAt,
        Long gmtLastLoginAt) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
