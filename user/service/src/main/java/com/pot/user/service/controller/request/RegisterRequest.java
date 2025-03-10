package com.pot.user.service.controller.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/3/10 22:54
 * @description: 用户注册
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    private String name;
    private String password;
    private String phone;
    private String email;
    private String code;
}
