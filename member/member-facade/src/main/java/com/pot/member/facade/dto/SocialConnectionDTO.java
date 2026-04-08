package com.pot.member.facade.dto;

import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;

@Builder
public record SocialConnectionDTO(
        Long id,
        Long memberId,
        String provider,
        String providerMemberId,
        String providerUsername,
        String providerEmail,
        String avatarUrl,
        Boolean isActive,
        Long boundAt,
        Long updatedAt,
        Long lastUsedAt,
        Boolean isPrimary,
        String status) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}