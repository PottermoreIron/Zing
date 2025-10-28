package com.pot.auth.service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/10/12 22:09
 * @description: 认证Token响应类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthToken {
    /**
     * 访问令牌
     */
    private String accessToken;
    /**
     * 刷新令牌
     */
    private String refreshToken;
    /**
     * 令牌类型
     */
    private String tokenType;
    /**
     * 访问令牌过期时间，单位秒
     */
    private Long accessExpiresIn;
    /**
     * 刷新令牌过期时间，单位秒
     */
    private Long refreshExpiresIn;
}
