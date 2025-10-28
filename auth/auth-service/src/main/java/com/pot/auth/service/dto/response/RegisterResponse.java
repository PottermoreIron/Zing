package com.pot.auth.service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.pot.auth.service.enums.RegisterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/10/12 22:18
 * @description: 注册响应类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterResponse {
    /**
     * 认证响应
     */
    @JsonUnwrapped
    private AuthResponse authResponse;
    /**
     * 注册类型
     */
    private RegisterType type;
    /**
     * 注册时间
     */
    private Long registerAt;
    /**
     * 响应消息
     */
    private String message;
}
