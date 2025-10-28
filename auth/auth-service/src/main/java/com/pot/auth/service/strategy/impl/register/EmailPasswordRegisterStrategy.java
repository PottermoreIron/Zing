package com.pot.auth.service.strategy.impl.register;

import com.pot.auth.service.dto.request.register.EmailPasswordRegisterRequest;
import com.pot.auth.service.enums.RegisterType;
import com.pot.auth.service.service.adapter.VerificationCodeAdapter;
import com.pot.auth.service.utils.UserTokenUtils;
import com.pot.member.facade.api.MemberFacade;
import com.pot.member.facade.dto.request.CreateMemberRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/10/19 21:38
 * @description: 邮箱密码注册策略
 */
@Slf4j
@Component
public class EmailPasswordRegisterStrategy extends AbstractRegisterStrategyImpl<EmailPasswordRegisterRequest> {

    public EmailPasswordRegisterStrategy(MemberFacade memberFacade, UserTokenUtils userTokenUtils,
                                         VerificationCodeAdapter verificationCodeAdapter) {
        super(memberFacade, userTokenUtils, verificationCodeAdapter);
    }

    @Override
    public RegisterType getRegisterType() {
        return RegisterType.EMAIL_PASSWORD;
    }

    @Override
    protected void validateBusinessRules(EmailPasswordRegisterRequest request) {
        // TODO: 检查邮箱是否已注册
        log.debug("校验邮箱是否存在: email={}", request.getEmail());
    }

    @Override
    protected void verifyCode(EmailPasswordRegisterRequest request) {
        // 邮箱密码注册不需要验证码
    }

    @Override
    protected void preRegister(EmailPasswordRegisterRequest request) {
        log.debug("邮箱密码注册前置处理: email={}", request.getEmail());
    }

    @Override
    protected CreateMemberRequest buildCreateMemberRequest(EmailPasswordRegisterRequest request) {
        return CreateMemberRequest.builder()
                .nickname(generateRandomNickname())
                .email(request.getEmail())
                .password(generateEncodedPassword(request.getPassword()))
                .build();
    }

    @Override
    protected void postRegister(EmailPasswordRegisterRequest request) {
        log.info("邮箱密码注册成功: email={}", request.getEmail());
    }
}
