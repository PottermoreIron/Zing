package com.pot.auth.service.strategy.impl.register;

import com.pot.auth.service.dto.request.register.PhoneCodeRegisterRequest;
import com.pot.auth.service.enums.RegisterType;
import com.pot.auth.service.service.adapter.VerificationCodeAdapter;
import com.pot.auth.service.utils.UserTokenUtils;
import com.pot.member.facade.api.MemberFacade;
import com.pot.member.facade.dto.request.CreateMemberRequest;
import com.pot.zing.framework.common.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/10/19 21:38
 * @description: 手机验证码注册策略
 */
@Slf4j
@Component
public class PhoneCodeRegisterStrategy extends AbstractRegisterStrategyImpl<PhoneCodeRegisterRequest> {

    public PhoneCodeRegisterStrategy(MemberFacade memberFacade, UserTokenUtils userTokenUtils,
                                     VerificationCodeAdapter verificationCodeAdapter) {
        super(memberFacade, userTokenUtils, verificationCodeAdapter);
    }

    @Override
    public RegisterType getRegisterType() {
        return RegisterType.PHONE_CODE;
    }

    @Override
    protected void validateBusinessRules(PhoneCodeRegisterRequest request) {
        R<Boolean> checkResult = memberFacade.checkPhoneExists(request.getPhone());
        if (!checkResult.isSuccess()) {
            throw new RuntimeException("检查手机号是否存在失败: " + checkResult.getMsg());
        }
        if (Boolean.TRUE.equals(checkResult.getData())) {
            throw new RuntimeException("手机号已被注册: " + request.getPhone());
        }
    }

    @Override
    protected void verifyCode(PhoneCodeRegisterRequest request) {
        // 验证手机验证码
        validateAndDeleteCode(request.getPhone(), request.getCode());
    }

    @Override
    protected void preRegister(PhoneCodeRegisterRequest request) {
        log.debug("手机验证码注册前置处理: phone={}", request.getPhone());
    }

    @Override
    protected CreateMemberRequest buildCreateMemberRequest(PhoneCodeRegisterRequest request) {
        return CreateMemberRequest.builder()
                .nickname(generateRandomNickname())
                .password(generateEncodedPassword(generateRandomPassword()))
                .phone(request.getPhone())
                .build();
    }

    @Override
    protected void postRegister(PhoneCodeRegisterRequest request) {
        log.info("手机验证码注册成功: phone={}", request.getPhone());
    }
}
