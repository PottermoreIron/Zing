package com.pot.member.facade.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/10/17 23:14
 * @description: 用户创建请求
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateMemberRequest {
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 密码
     */
    private String password;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 手机号
     */
    private String phone;
}
