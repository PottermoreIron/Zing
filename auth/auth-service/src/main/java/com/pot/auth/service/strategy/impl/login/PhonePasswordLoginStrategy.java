package com.pot.auth.service.strategy.impl.login;

import com.pot.auth.service.dto.request.login.PhonePasswordLoginRequest;
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
 * @description: 手机号密码登录策略
 */
@Slf4j
@Component
public class PhonePasswordLoginStrategy extends AbstractLoginStrategyImpl<PhonePasswordLoginRequest> {

    public PhonePasswordLoginStrategy(
            MemberFacade memberFacade,
            UserTokenUtils userTokenUtils,
            VerificationCodeAdapter verificationCodeAdapter) {
        super(memberFacade, userTokenUtils, verificationCodeAdapter);
    }

    @Override
    public LoginType getLoginType() {
        return LoginType.PHONE_PASSWORD;
    }

    @Override
    protected void validateBusinessRules(PhonePasswordLoginRequest request) {
        log.debug("手机号密码登录业务规则校验通过: phone={}", request.getPhone());
    }

    @Override
    protected MemberDTO getMember(PhonePasswordLoginRequest request) {
        R<MemberDTO> result = memberFacade.getMemberByPhone(request.getPhone());
        return fetchMemberSafely(result, "手机号或密码错误");
    }

    @Override
    protected void validateCredentials(PhonePasswordLoginRequest request, MemberDTO memberDTO) {
        validatePassword(request.getPassword(), memberDTO.getPassword());
    }
}

