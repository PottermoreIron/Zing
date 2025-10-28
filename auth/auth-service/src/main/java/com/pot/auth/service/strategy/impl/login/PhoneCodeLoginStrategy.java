package com.pot.auth.service.strategy.impl.login;

import com.pot.auth.service.dto.request.login.PhoneCodeLoginRequest;
import com.pot.auth.service.enums.LoginType;
import com.pot.auth.service.service.adapter.VerificationCodeAdapter;
import com.pot.auth.service.utils.UserTokenUtils;
import com.pot.member.facade.api.MemberFacade;
import com.pot.member.facade.dto.MemberDTO;
import com.pot.zing.framework.common.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/10/20
 * @description: 手机验证码登录策略
 */
@Slf4j
@Component
public class PhoneCodeLoginStrategy extends AbstractLoginStrategyImpl<PhoneCodeLoginRequest> {

    public PhoneCodeLoginStrategy(
            MemberFacade memberFacade,
            UserTokenUtils userTokenUtils,
            VerificationCodeAdapter verificationCodeAdapter) {
        super(memberFacade, userTokenUtils, verificationCodeAdapter);
    }

    @Override
    public LoginType getLoginType() {
        return LoginType.PHONE_CODE;
    }

    @Override
    protected void validateBusinessRules(PhoneCodeLoginRequest request) {
        log.debug("手机验证码登录业务规则校验通过: phone={}", request.getPhone());
    }

    @Override
    protected void verifyCode(PhoneCodeLoginRequest request) {
        // 验证验证码
        validateAndDeleteCode(request.getPhone(), request.getCode());
    }

    @Override
    protected MemberDTO getMember(PhoneCodeLoginRequest request) {
        R<MemberDTO> result = memberFacade.getMemberByPhone(request.getPhone());
        return fetchMemberSafely(result, "手机号未注册");
    }

    @Override
    protected void validateCredentials(PhoneCodeLoginRequest request, MemberDTO memberDTO) {
        // 验证码登录不需要验证密码，验证码已在 verifyCode 方法中验证
        log.debug("手机验证码登录，跳过密码验证");
    }
}

