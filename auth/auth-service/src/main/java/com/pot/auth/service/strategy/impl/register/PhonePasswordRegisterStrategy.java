package com.pot.auth.service.strategy.impl.register;

import com.pot.auth.service.dto.request.register.PhonePasswordRegisterRequest;
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
 * @description: 手机号密码注册策略
 */
@Slf4j
@Component
public class PhonePasswordRegisterStrategy extends AbstractRegisterStrategyImpl<PhonePasswordRegisterRequest> {

    public PhonePasswordRegisterStrategy(MemberFacade memberFacade, UserTokenUtils userTokenUtils,
                                         VerificationCodeAdapter verificationCodeAdapter) {
        super(memberFacade, userTokenUtils, verificationCodeAdapter);
    }

    @Override
    public RegisterType getRegisterType() {
        return RegisterType.PHONE_PASSWORD;
    }

    @Override
    protected void validateBusinessRules(PhonePasswordRegisterRequest request) {
        // TODO: 检查手机号是否已注册
        log.debug("校验手机号是否存在: phone={}", request.getPhone());
    }

    @Override
    protected void verifyCode(PhonePasswordRegisterRequest request) {
        // 手机号密码注册不需要验证码
    }

    @Override
    protected void preRegister(PhonePasswordRegisterRequest request) {
        log.debug("手机号密码注册前置处理: phone={}", request.getPhone());
    }

    @Override
    protected CreateMemberRequest buildCreateMemberRequest(PhonePasswordRegisterRequest request) {
        return CreateMemberRequest.builder()
                .nickname(generateRandomNickname())
                .phone(request.getPhone())
                .password(generateEncodedPassword(request.getPassword()))
                .build();
    }

    @Override
    protected void postRegister(PhonePasswordRegisterRequest request) {
        log.info("手机号密码注册成功: phone={}", request.getPhone());
    }
}
