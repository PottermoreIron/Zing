package com.pot.auth.service.strategy.impl.signinorregister;

import com.pot.auth.service.dto.request.login.PhoneCodeLoginRequest;
import com.pot.auth.service.dto.request.register.PhoneCodeRegisterRequest;
import com.pot.auth.service.dto.request.signinorregister.PhoneCodeSignInOrRegisterRequest;
import com.pot.auth.service.dto.response.AuthResponse;
import com.pot.auth.service.dto.response.RegisterResponse;
import com.pot.auth.service.enums.SignInOrRegisterType;
import com.pot.auth.service.enums.VerificationBizType;
import com.pot.auth.service.service.LoginService;
import com.pot.auth.service.service.RegisterService;
import com.pot.auth.service.service.adapter.VerificationCodeAdapter;
import com.pot.member.facade.api.MemberFacade;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/10/23
 * @description: 手机验证码一键登录/注册策略
 *
 * <p>业务逻辑：</p>
 * <ol>
 *   <li>验证手机验证码的有效性</li>
 *   <li>检查手机号是否已注册</li>
 *   <li>已注册：复用 PhoneCodeLoginStrategy 进行登录</li>
 *   <li>未注册：复用 PhoneCodeRegisterStrategy 进行注册并返回Token</li>
 * </ol>
 */
@Slf4j
@Component
public class PhoneCodeSignInOrRegisterStrategy
        extends AbstractSignInOrRegisterStrategy<PhoneCodeSignInOrRegisterRequest> {

    public PhoneCodeSignInOrRegisterStrategy(
            MemberFacade memberFacade,
            LoginService loginService,
            RegisterService registerService,
            VerificationCodeAdapter verificationCodeAdapter) {
        super(memberFacade, loginService, registerService, verificationCodeAdapter);
    }

    @Override
    public SignInOrRegisterType getType() {
        return SignInOrRegisterType.PHONE_CODE;
    }

    @Override
    protected void validateCredentials(PhoneCodeSignInOrRegisterRequest request) {
        // 验证手机验证码
        log.debug("验证手机验证码: phone={}", request.getPhone());

        boolean isValid = verificationCodeAdapter.verifyAndDelete(
                request.getPhone(),
                request.getCode(),
                VerificationBizType.SIGN_IN_OR_REGISTER
        );

        if (!isValid) {
            throw new BusinessException("验证码无效或已过期");
        }

        log.debug("手机验证码验证成功: phone={}", request.getPhone());
    }

    @Override
    protected String extractUniqueIdentifier(PhoneCodeSignInOrRegisterRequest request) {
        return request.getPhone();
    }

    @Override
    protected boolean checkUserExists(String phone) {
        R<Boolean> result = memberFacade.checkPhoneExists(phone);

        if (!result.isSuccess()) {
            throw new BusinessException("检查用户状态失败: " + result.getMsg());
        }

        return Boolean.TRUE.equals(result.getData());
    }

    @Override
    protected AuthResponse performLogin(PhoneCodeSignInOrRegisterRequest request) {
        log.debug("构建手机验证码登录请求: phone={}", request.getPhone());

        // 构造登录请求（复用现有 PhoneCodeLoginStrategy）
        PhoneCodeLoginRequest loginRequest = new PhoneCodeLoginRequest();
        loginRequest.setPhone(request.getPhone());
        loginRequest.setCode(request.getCode());

        return loginService.login(loginRequest);
    }

    @Override
    protected AuthResponse performRegisterAndLogin(PhoneCodeSignInOrRegisterRequest request) {
        log.debug("构建手机验证码注册请求: phone={}", request.getPhone());

        // 构造注册请求（复用现有 PhoneCodeRegisterStrategy）
        PhoneCodeRegisterRequest registerRequest = new PhoneCodeRegisterRequest();
        registerRequest.setPhone(request.getPhone());
        registerRequest.setCode(request.getCode());

        // 执行注册
        RegisterResponse registerResponse = registerService.register(registerRequest);

        // 提取认证响应
        return extractAuthResponse(registerResponse);
    }

    @Override
    protected void postProcess(PhoneCodeSignInOrRegisterRequest request,
                               AuthResponse response,
                               boolean isNewUser) {
        if (isNewUser) {
            log.info("新用户通过手机验证码注册成功: phone={}, memberId={}",
                    request.getPhone(),
                    response.getUserInfo() != null ? response.getUserInfo().getMemberId() : "unknown");

            // TODO: 可扩展：发送欢迎短信、推送通知、赠送新人礼包等
        } else {
            log.info("用户通过手机验证码登录成功: phone={}, memberId={}",
                    request.getPhone(),
                    response.getUserInfo() != null ? response.getUserInfo().getMemberId() : "unknown");
        }
    }
}