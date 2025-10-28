package com.pot.auth.service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/10/12 22:04
 * @description: 认证响应类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    /**
     * 认证Token
     */
    private AuthToken authToken;
    /**
     * 用户信息
     */
    private AuthUserInfoVO userInfo;
    /**
     * 响应时间戳
     */
    private Long timestamp;
}
