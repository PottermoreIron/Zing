package com.pot.auth.service.strategy.impl.register;

import com.pot.auth.service.dto.request.register.EmailCodeRegisterRequest;
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
 * @created: 2025/10/19 21:39
 * @description: 邮箱验证码注册策略
 */
@Slf4j
@Component
public class EmailCodeRegisterStrategy extends AbstractRegisterStrategyImpl<EmailCodeRegisterRequest> {

    public EmailCodeRegisterStrategy(MemberFacade memberFacade, UserTokenUtils userTokenUtils,
                                     VerificationCodeAdapter verificationCodeAdapter) {
        super(memberFacade, userTokenUtils, verificationCodeAdapter);
    }

    @Override
    public RegisterType getRegisterType() {
        return RegisterType.EMAIL_CODE;
    }

    @Override
    protected void validateBusinessRules(EmailCodeRegisterRequest request) {
        R<Boolean> checkResult = memberFacade.checkEmailExists(request.getEmail());
        if (!checkResult.isSuccess()) {
            throw new RuntimeException("检查邮箱是否存在失败: " + checkResult.getMsg());
        }
        if (Boolean.TRUE.equals(checkResult.getData())) {
            throw new RuntimeException("邮箱已被注册: " + request.getEmail());
        }
    }

    @Override
    protected void verifyCode(EmailCodeRegisterRequest request) {
        // 验证邮箱验证码
        validateAndDeleteCode(request.getEmail(), request.getCode());
    }

    @Override
    protected void preRegister(EmailCodeRegisterRequest request) {
        log.debug("邮箱验证码注册前置处理: email={}", request.getEmail());
    }

    @Override
    protected CreateMemberRequest buildCreateMemberRequest(EmailCodeRegisterRequest request) {
        return CreateMemberRequest.builder()
                .nickname(generateRandomNickname())
                .password(generateEncodedPassword(generateRandomPassword()))
                .email(request.getEmail())
                .build();
    }

    @Override
    protected void postRegister(EmailCodeRegisterRequest request) {
        log.info("邮箱验证码注册成功: email={}", request.getEmail());
    }
}
