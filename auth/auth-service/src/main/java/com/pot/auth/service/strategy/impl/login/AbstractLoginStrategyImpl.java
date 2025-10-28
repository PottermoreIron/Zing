package com.pot.auth.service.strategy.impl.login;

import com.pot.auth.service.dto.request.login.LoginRequest;
import com.pot.auth.service.dto.response.AuthResponse;
import com.pot.auth.service.dto.response.AuthToken;
import com.pot.auth.service.dto.response.AuthUserInfoVO;
import com.pot.auth.service.enums.LoginType;
import com.pot.auth.service.enums.VerificationBizType;
import com.pot.auth.service.service.adapter.VerificationCodeAdapter;
import com.pot.auth.service.strategy.impl.LoginStrategy;
import com.pot.auth.service.utils.ConvertUtils;
import com.pot.auth.service.utils.UserTokenUtils;
import com.pot.member.facade.api.MemberFacade;
import com.pot.member.facade.dto.MemberDTO;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.common.util.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: Pot
 * @created: 2025/10/20
 * @description: 抽象登录策略实现 - 提供登录流程的模板方法
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractLoginStrategyImpl<T extends LoginRequest>
        implements LoginStrategy<T> {

    protected final MemberFacade memberFacade;
    protected final UserTokenUtils userTokenUtils;
    protected final VerificationCodeAdapter verificationCodeAdapter;

    @Override
    public AuthResponse login(T request) {
        log.info("开始登录流程: type={}", request.getType());

        // 1. 业务规则校验
        validateBusinessRules(request);

        // 2. 验证码校验（如果需要）
        verifyCode(request);

        // 3. 获取用户信息
        MemberDTO memberDTO = getMember(request);
        if (memberDTO == null) {
            throw new BusinessException("用户不存在");
        }

        // 4. 验证账号状态
        validateAccountStatus(memberDTO);

        // 5. 验证凭证（密码或验证码)
        validateCredentials(request, memberDTO);

        // 6. 登录前置处理（可选：记录登录日志、更新最后登录时间等）
        preLogin(request, memberDTO);

        // 7. 构建用户信息VO
        AuthUserInfoVO authUserInfoVO = ConvertUtils.toUserInfoVO(memberDTO);

        // 8. 生成Token
        AuthToken authToken = userTokenUtils.createAccessTokenAndRefreshToken(authUserInfoVO.getMemberId());

        // 9. 登录后置处理
        postLogin(request, memberDTO);

        // 10. 构建并返回响应
        AuthResponse response = buildAuthResponse(authToken, authUserInfoVO);

        log.info("登录流程完成: type={}, memberId={}", request.getType(), memberDTO.getMemberId());
        return response;
    }

    /**
     * 获取登录类型（子类实现）
     */
    public abstract LoginType getLoginType();

    /**
     * 验证业务规则（子类实现具体的业务校验逻辑）
     */
    protected abstract void validateBusinessRules(T request);

    /**
     * 验证验证码（基于验证码的登录方式需要实现）
     */
    protected void verifyCode(T request) {
        // 默认不需要验证码，子类按需覆盖
    }

    /**
     * 获取用户信息（子类实现）
     */
    protected abstract MemberDTO getMember(T request);

    /**
     * 验证凭证（密码或验证码）
     */
    protected abstract void validateCredentials(T request, MemberDTO memberDTO);

    /**
     * 登录前置处理
     */
    protected void preLogin(T request, MemberDTO memberDTO) {
        // 默认空实现，子类可以覆盖
        log.debug("执行登录前置处理: memberId={}", memberDTO.getMemberId());
    }

    /**
     * 登录后置处理
     */
    protected void postLogin(T request, MemberDTO memberDTO) {
        // 默认空实现，子类可以覆盖
        log.debug("执行登录后置处理: memberId={}", memberDTO.getMemberId());
    }

    /**
     * 验证账号状态
     */
    protected void validateAccountStatus(MemberDTO memberDTO) {
        if (memberDTO.getStatus() == null) {
            throw new BusinessException("账号状态异常");
        }

        // 假设状态: ACTIVE-正常, LOCKED-锁定, DISABLED-禁用
        String status = memberDTO.getStatus();
        switch (status) {
            case "LOCKED":
                throw new BusinessException("账号已被锁定，请联系管理员");
            case "DISABLED":
                throw new BusinessException("账号已被禁用");
            case "ACTIVE":
                // 正常状态，继续
                break;
            default:
                throw new BusinessException("账号状态异常: " + status);
        }
    }

    /**
     * 验证密码
     */
    protected void validatePassword(String rawPassword, String encodedPassword) {
        if (!PasswordUtils.matches(rawPassword, encodedPassword)) {
            throw new BusinessException("密码错误");
        }
    }

    /**
     * 验证并删除验证码
     */
    protected void validateAndDeleteCode(String target, String code) {
        verificationCodeAdapter.validateCode(
                target,
                code,
                VerificationBizType.LOGIN.getCode()
        );
    }

    /**
     * 构建认证响应
     */
    protected AuthResponse buildAuthResponse(AuthToken authToken, AuthUserInfoVO userInfo) {
        return AuthResponse.builder()
                .authToken(authToken)
                .userInfo(userInfo)
                .timestamp(System.currentTimeMillis() / 1000)
                .build();
    }

    /**
     * 安全地获取用户（处理RPC调用）
     */
    protected MemberDTO fetchMemberSafely(R<MemberDTO> result, String errorMessage) {
        if (!result.isSuccess() || result.getData() == null) {
            throw new BusinessException(errorMessage);
        }
        return result.getData();
    }
}
