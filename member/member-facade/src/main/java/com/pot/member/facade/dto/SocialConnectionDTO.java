package com.pot.member.facade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialConnectionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

        private Long id;

        private Long memberId;

        private String provider;

        private String providerMemberId;

        private String providerUsername;

        private String providerEmail;

        private String avatarUrl;

        private Boolean isActive;

        private Long boundAt;

        private Long updatedAt;

        private Long lastUsedAt;

        private Boolean isPrimary;

        private String status;
}