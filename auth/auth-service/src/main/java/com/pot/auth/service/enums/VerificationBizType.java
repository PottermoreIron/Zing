package com.pot.auth.service.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/10/19 17:54
 * @description: 验证码业务类型
 */
@Getter
@RequiredArgsConstructor
public enum VerificationBizType {

    REGISTER("register", "注册"),
    LOGIN("login", "登录"),
    RESET_PASSWORD("reset_password", "重置密码"),
    CHANGE_PHONE("change_phone", "更换手机号"),
    CHANGE_EMAIL("change_email", "更换邮箱"),
    BIND_ACCOUNT("bind_account", "绑定账号"),
    SIGN_IN_OR_REGISTER("sign_in_or_register", "一键登录注册");

    private final String code;
    private final String desc;
}
