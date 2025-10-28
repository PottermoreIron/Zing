package com.pot.auth.service.strategy.impl.register;

import com.pot.auth.service.dto.request.register.UserNamePasswordRegisterRequest;
import com.pot.auth.service.enums.RegisterType;
import com.pot.auth.service.service.adapter.VerificationCodeAdapter;
import com.pot.auth.service.utils.UserTokenUtils;
import com.pot.member.facade.api.MemberFacade;
import com.pot.member.facade.dto.request.CreateMemberRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/10/19 21:37
 * @description: 用户名密码注册策略
 */
@Slf4j
@Component
public class UsernamePasswordRegisterStrategy extends AbstractRegisterStrategyImpl<UserNamePasswordRegisterRequest> {

    public UsernamePasswordRegisterStrategy(MemberFacade memberFacade, UserTokenUtils userTokenUtils,
                                            VerificationCodeAdapter verificationCodeAdapter) {
        super(memberFacade, userTokenUtils, verificationCodeAdapter);
    }

    @Override
    public RegisterType getRegisterType() {
        return RegisterType.USERNAME_PASSWORD;
    }

    @Override
    protected void validateBusinessRules(UserNamePasswordRegisterRequest request) {
        // TODO: 检查用户名是否已存在
        log.debug("校验用户名是否存在: username={}", request.getUsername());
    }

    @Override
    protected void verifyCode(UserNamePasswordRegisterRequest request) {
        // 用户名密码注册不需要验证码
    }

    @Override
    protected void preRegister(UserNamePasswordRegisterRequest request) {
        log.debug("用户名密码注册前置处理: username={}", request.getUsername());
    }

    @Override
    protected CreateMemberRequest buildCreateMemberRequest(UserNamePasswordRegisterRequest request) {
        return CreateMemberRequest.builder()
                .nickname(request.getUsername())
                .password(generateEncodedPassword(request.getPassword()))
                .build();
    }

    @Override
    protected void postRegister(UserNamePasswordRegisterRequest request) {
        log.info("用户名密码注册成功: username={}", request.getUsername());
    }
}
