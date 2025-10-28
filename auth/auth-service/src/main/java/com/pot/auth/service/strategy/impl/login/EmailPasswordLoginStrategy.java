package com.pot.auth.service.strategy.impl.login;

import com.pot.auth.service.dto.request.login.EmailPasswordLoginRequest;
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
 * @description: 邮箱密码登录策略
 */
@Slf4j
@Component
public class EmailPasswordLoginStrategy extends AbstractLoginStrategyImpl<EmailPasswordLoginRequest> {

    public EmailPasswordLoginStrategy(
            MemberFacade memberFacade,
            UserTokenUtils userTokenUtils,
            VerificationCodeAdapter verificationCodeAdapter) {
        super(memberFacade, userTokenUtils, verificationCodeAdapter);
    }

    @Override
    public LoginType getLoginType() {
        return LoginType.EMAIL_PASSWORD;
    }

    @Override
    protected void validateBusinessRules(EmailPasswordLoginRequest request) {
        log.debug("邮箱密码登录业务规则校验通过: email={}", request.getEmail());
    }

    @Override
    protected MemberDTO getMember(EmailPasswordLoginRequest request) {
        R<MemberDTO> result = memberFacade.getMemberByEmail(request.getEmail());
        return fetchMemberSafely(result, "邮箱或密码错误");
    }

    @Override
    protected void validateCredentials(EmailPasswordLoginRequest request, MemberDTO memberDTO) {
        validatePassword(request.getPassword(), memberDTO.getPassword());
    }
}

