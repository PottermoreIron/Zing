package com.pot.member.service.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取会员查询
 * 
 * @author Pot
 * @since 2026-01-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetMemberQuery {

    private Long memberId;
    private String email;
    private String phoneNumber;
    private String username;
}
