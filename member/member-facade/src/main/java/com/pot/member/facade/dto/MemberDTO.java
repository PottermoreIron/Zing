package com.pot.member.facade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * RPC DTO for core member data shared across services.
 *
 * @author Pot
 * @since 2026-03-18
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Stable member identifier. */
    private Long memberId;

    /** Member display nickname. */
    private String nickname;

    /** Email address. */
    private String email;

    /** Phone number. */
    private String phone;

    /** Avatar URL. */
    private String avatarUrl;

    /** Account status such as ACTIVE, LOCKED, or DISABLED. */
    private String status;

    /** Assigned role codes. */
    private Set<String> roleCodes;

    /** Assigned permission codes. */
    private Set<String> permissionCodes;

    /** Registration timestamp in milliseconds since the epoch. */
    private Long gmtCreatedAt;

    /** Last login timestamp in milliseconds since the epoch. */
    private Long gmtLastLoginAt;
}
