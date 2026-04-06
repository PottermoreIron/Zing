package com.pot.member.service.application.assembler;

import com.pot.member.service.application.dto.MemberDTO;
import com.pot.member.service.domain.model.member.MemberAggregate;
import org.springframework.stereotype.Component;

@Component
public class MemberAssembler {

        public MemberDTO toDTO(MemberAggregate aggregate) {
        if (aggregate == null) {
            return null;
        }

        return MemberDTO.builder()
                .memberId(aggregate.getMemberId() != null ? aggregate.getMemberId().value() : null)
                .nickname(aggregate.getNickname() != null ? aggregate.getNickname().getValue() : null)
                .email(aggregate.getEmail() != null ? aggregate.getEmail().getValue() : null)
                .phoneNumber(aggregate.getPhoneNumber() != null ? aggregate.getPhoneNumber().getValue() : null)
                .status(aggregate.getStatus() != null ? aggregate.getStatus().name() : null)
                .roleIds(aggregate.getRoleIds())
                .createdAt(aggregate.getCreatedAt())
                .updatedAt(aggregate.getUpdatedAt())
                .lastLoginAt(aggregate.getLastLoginAt())
                .build();
    }
}
