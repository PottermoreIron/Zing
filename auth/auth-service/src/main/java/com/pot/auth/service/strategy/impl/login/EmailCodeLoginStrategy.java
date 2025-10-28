package com.pot.auth.service.strategy.impl.login;

import com.pot.auth.service.dto.request.login.EmailCodeLoginRequest;
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
 * @description: 邮箱验证码登录策略
 */
@Slf4j
@Component
public class EmailCodeLoginStrategy extends AbstractLoginStrategyImpl<EmailCodeLoginRequest> {

    public EmailCodeLoginStrategy(
            MemberFacade memberFacade,
            UserTokenUtils userTokenUtils,
            VerificationCodeAdapter verificationCodeAdapter) {
        super(memberFacade, userTokenUtils, verificationCodeAdapter);
    }

    @Override
    public LoginType getLoginType() {
        return LoginType.EMAIL_CODE;
    }

    @Override
    protected void validateBusinessRules(EmailCodeLoginRequest request) {
        log.debug("邮箱验证码登录业务规则校验通过: email={}", request.getEmail());
    }

    @Override
    protected void verifyCode(EmailCodeLoginRequest request) {
        // 验证验证码
        validateAndDeleteCode(request.getEmail(), request.getCode());
    }

    @Override
    protected MemberDTO getMember(EmailCodeLoginRequest request) {
        R<MemberDTO> result = memberFacade.getMemberByEmail(request.getEmail());
        return fetchMemberSafely(result, "邮箱未注册");
    }

    @Override
    protected void validateCredentials(EmailCodeLoginRequest request, MemberDTO memberDTO) {
        // 验证码登录不需要验证密码，验证码已在 verifyCode 方法中验证
        log.debug("邮箱验证码登录，跳过密码验证");
    }
}

