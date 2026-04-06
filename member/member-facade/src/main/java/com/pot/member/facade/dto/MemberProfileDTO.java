package com.pot.member.facade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberProfileDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long memberId;
    private String nickname;
    private String firstName;
    private String lastName;
    private Integer gender;
    private String birthDate;
    private String avatarUrl;
    private String bio;
    private String countryCode;
    private String region;
    private String city;
    private String timezone;
    private String locale;
}
