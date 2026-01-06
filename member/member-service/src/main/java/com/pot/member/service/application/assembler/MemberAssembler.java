package com.pot.member.service.application.assembler;

import com.pot.member.service.application.dto.MemberDTO;
import com.pot.member.service.domain.model.member.MemberAggregate;
import org.springframework.stereotype.Component;

/**
 * 会员装配器 - 负责聚合根与DTO之间的转换
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Component
public class MemberAssembler {

    /**
     * 将聚合根转换为DTO
     */
    public MemberDTO toDTO(MemberAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }

        return MemberDTO.builder()
                .memberId(aggregate.getMemberId() != null ? aggregate.getMemberId().value() : null)
                .username(aggregate.getUsername() != null ? aggregate.getUsername().getValue() : null)
                .email(aggregate.getEmail() != null ? aggregate.getEmail().getValue() : null)
                .phoneNumber(aggregate.getPhoneNumber() != null ? aggregate.getPhoneNumber().getValue() : null)
                .avatar(aggregate.getAvatar())
                .bio(aggregate.getBio())
                .status(aggregate.getStatus() != null ? aggregate.getStatus().name() : null)
                .roleIds(aggregate.getRoleIds())
                .createdAt(aggregate.getCreatedAt())
                .updatedAt(aggregate.getUpdatedAt())
                .lastLoginAt(aggregate.getLastLoginAt())
                .build();
    }
}
