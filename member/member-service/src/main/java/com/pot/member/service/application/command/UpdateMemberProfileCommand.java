package com.pot.member.service.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberProfileCommand {

    private Long memberId;
    private String nickname;
    private String firstName;
    private String lastName;
    private Integer gender;
    private String birthDate;
    private String bio;
    private String countryCode;
    private String region;
    private String city;
    private String timezone;
    private String locale;
}
