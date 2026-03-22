package com.pot.member.facade.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 会员个人资料 DTO（对外 RPC 契约）
 *
 * <p>
 * 将 profile 信息独立拆出，避免核心认证数据与 profile 数据耦合在一起。
 *
 * @author Pot
 * @since 2026-03-18
 */
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
