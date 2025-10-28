package com.pot.auth.service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/10/12 22:13
 * @description: 用户信息VO类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthUserInfoVO {
    private Long memberId;
    private String nickname;
    private String email;
    private String phone;
    private String avatarUrl;
    private Integer gender;
    private String status;
    private Long gmtCreatedAt;
}
