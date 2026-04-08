package com.pot.member.service.application.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
public record MemberDTO(
        Long memberId,
        String nickname,
        String email,
        String phoneNumber,
        String status,
        Set<Long> roleIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLoginAt) {
}
