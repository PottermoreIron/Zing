package com.pot.member.service.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/3/23 16:27
 * @description: 登录注册成功返回的两个Token
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Tokens {
    private String accessToken;
    private String refreshToken;
}
