package com.pot.auth.service.strategy.impl.register;

import com.pot.auth.service.dto.request.register.RegisterRequest;
import com.pot.auth.service.dto.response.AuthResponse;
import com.pot.auth.service.dto.response.AuthToken;
import com.pot.auth.service.dto.response.AuthUserInfoVO;
import com.pot.auth.service.dto.response.RegisterResponse;
import com.pot.auth.service.enums.RegisterType;
import com.pot.auth.service.enums.VerificationBizType;
import com.pot.auth.service.service.adapter.VerificationCodeAdapter;
import com.pot.auth.service.strategy.RegisterStrategy;
import com.pot.auth.service.utils.ConvertUtils;
import com.pot.auth.service.utils.UserTokenUtils;
import com.pot.member.facade.api.MemberFacade;
import com.pot.member.facade.dto.MemberDTO;
import com.pot.member.facade.dto.request.CreateMemberRequest;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.common.util.PasswordUtils;
import com.pot.zing.framework.common.util.RandomUtils;
import com.pot.zing.framework.common.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: Pot
 * @created: 2025/10/14 23:13
 * @description: 抽象注册策略类
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractRegisterStrategyImpl<T extends RegisterRequest>
        implements RegisterStrategy<T> {
    protected final MemberFacade memberFacade;
    protected final UserTokenUtils userTokenUtils;
    protected final VerificationCodeAdapter verificationCodeAdapter;

    @Override
    public RegisterResponse register(T request) {
        log.info("开始注册流程: type={}", request.getType());

        // 1. 通用校验 (由 @Valid 完成)

        // 2. 业务规则校验
        validateBusinessRules(request);

        // 3. 验证码校验
        verifyCode(request);

        // 4. 注册前置处理
        preRegister(request);

        // 5. 构建会员创建请求
        CreateMemberRequest createRequest = buildCreateMemberRequest(request);

        // 6. 调用 member 服务创建用户
        R<MemberDTO> result = memberFacade.createMember(createRequest);
        if (!result.isSuccess() || result.getData() == null) {
            throw new BusinessException("创建会员失败: " + result.getMsg());
        }
        MemberDTO memberDTO = result.getData();
        AuthUserInfoVO authUserInfoVO = ConvertUtils.toUserInfoVO(memberDTO);

        // 7. 注册后置处理
        postRegister(request);

        // 8. 构建并返回响应
        RegisterResponse response = buildRegisterResponse(authUserInfoVO);


        log.info("注册流程完成: type={}", request.getType());
        return response;
    }


    /**
     * 获取注册类型（子类实现）
     */
    public abstract RegisterType getRegisterType();

    /**
     * 验证业务规则
     */
    protected abstract void validateBusinessRules(T request);

    /**
     * 验证验证码 (由子类实现具体的验证逻辑)
     */
    protected abstract void verifyCode(T request);

    /**
     * 注册前置处理
     */
    protected abstract void preRegister(T request);

    /**
     * 构建会员创建请求
     */
    protected abstract CreateMemberRequest buildCreateMemberRequest(T request);

    /**
     * 注册后置处理
     */
    protected abstract void postRegister(T request);

    /**
     * 构建注册响应
     */
    protected RegisterResponse buildRegisterResponse(AuthUserInfoVO authUserInfoVO) {
        // 生成令牌
        AuthToken authToken = userTokenUtils.createAccessTokenAndRefreshToken(authUserInfoVO.getMemberId());

        // 获取当前时间戳
        long currentTimestamp = TimeUtils.currentTimestamp();

        // 构建认证响应
        AuthResponse authResponse = AuthResponse.builder()
                .authToken(authToken)
                .userInfo(authUserInfoVO)
                .timestamp(currentTimestamp)
                .build();

        // 构建注册响应
        return RegisterResponse.builder()
                .authResponse(authResponse)
                .type(getRegisterType())
                .registerAt(currentTimestamp)
                .message("注册成功")
                .build();
    }

    // 工具方法
    protected String generateRandomNickname() {
        return RandomUtils.generateRandomNickname();
    }

    protected String generateEncodedPassword(String rawPassword) {
        return PasswordUtils.encodePassword(rawPassword);
    }

    protected String generateRandomPassword() {
        return PasswordUtils.generateDefaultPassword();
    }

    /**
     * 验证并删除验证码
     */
    protected void validateAndDeleteCode(String target, String code) {
        verificationCodeAdapter.validateCode(
                target,
                code,
                VerificationBizType.REGISTER.getCode()
        );
    }

    protected void handleRegisterFailure(T request, Exception e) {
        // 默认空实现，子类可选择性覆盖
        log.warn("注册失败处理: type={}", request.getType());
    }

}
