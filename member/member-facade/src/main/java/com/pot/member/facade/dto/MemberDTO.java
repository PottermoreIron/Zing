package com.pot.member.facade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/10/19 22:18
 * @description: member的dto
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberDTO {
    private Long memberId;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String password;
    private String avatarUrl;
    private Integer gender;
    private String status;
    private Long gmtEmailVerifiedAt;
    private Long gmtPhoneVerifiedAt;
    private Long gmtCreatedAt;
}
