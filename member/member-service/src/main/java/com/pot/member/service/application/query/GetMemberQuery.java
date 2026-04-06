package com.pot.member.service.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetMemberQuery {

    private Long memberId;
    private String email;
    private String phoneNumber;
    private String nickname;
}
