package com.pot.auth.service.strategy.impl.login;

import com.pot.auth.service.dto.request.login.UserNamePasswordLoginRequest;
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
 * @description: 用户名密码登录策略
 */
@Slf4j
@Component
public class UsernamePasswordLoginStrategy extends AbstractLoginStrategyImpl<UserNamePasswordLoginRequest> {

    public UsernamePasswordLoginStrategy(
            MemberFacade memberFacade,
            UserTokenUtils userTokenUtils,
            VerificationCodeAdapter verificationCodeAdapter) {
        super(memberFacade, userTokenUtils, verificationCodeAdapter);
    }

    @Override
    public LoginType getLoginType() {
        return LoginType.USERNAME_PASSWORD;
    }

    @Override
    protected void validateBusinessRules(UserNamePasswordLoginRequest request) {
        // @Valid 已经完成了基本校验
        log.debug("用户名密码登录业务规则校验通过: username={}", request.getUsername());
    }

    @Override
    protected MemberDTO getMember(UserNamePasswordLoginRequest request) {
        R<MemberDTO> result = memberFacade.getMemberByUsername(request.getUsername());
        return fetchMemberSafely(result, "用户名或密码错误");
    }

    @Override
    protected void validateCredentials(UserNamePasswordLoginRequest request, MemberDTO memberDTO) {
        validatePassword(request.getPassword(), memberDTO.getPassword());
    }
}