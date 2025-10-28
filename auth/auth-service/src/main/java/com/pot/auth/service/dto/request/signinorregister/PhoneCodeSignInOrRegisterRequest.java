package com.pot.auth.service.dto.request.signinorregister;

import com.pot.auth.service.enums.SignInOrRegisterType;
import com.pot.zing.framework.common.util.ValidationUtils;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: Pot
 * @created: 2025/10/23
 * @description: 手机验证码一键登录/注册请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PhoneCodeSignInOrRegisterRequest extends SignInOrRegisterRequest {

    @Pattern(regexp = ValidationUtils.PHONE_REGEX, message = "手机号格式不正确")
    private String phone;
    @Size(min = 6, max = 6, message = "验证码长度为6位")
    private String code;
    /**
     * 可选：新用户注册时的昵称（如果不提供则自动生成）
     */
    private String nickname;
    /**
     * 可选：新用户注册时的头像URL
     */
    private String avatarUrl;

    public PhoneCodeSignInOrRegisterRequest() {
        this.type = SignInOrRegisterType.PHONE_CODE;
    }
}