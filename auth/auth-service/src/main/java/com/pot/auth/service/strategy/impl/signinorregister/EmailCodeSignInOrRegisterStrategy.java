package com.pot.auth.service.strategy.impl.signinorregister;

import com.pot.auth.service.dto.request.login.EmailCodeLoginRequest;
import com.pot.auth.service.dto.request.register.EmailCodeRegisterRequest;
import com.pot.auth.service.dto.request.signinorregister.EmailCodeSignInOrRegisterRequest;
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
 * @description: 邮箱验证码一键登录/注册策略
 *
 * <p>业务逻辑：</p>
 * <ol>
 *   <li>验证邮箱验证码的有效性</li>
 *   <li>检查邮箱是否已注册</li>
 *   <li>已注册：复用 EmailCodeLoginStrategy 进行登录</li>
 *   <li>未注册：复用 EmailCodeRegisterStrategy 进行注册并返回Token</li>
 * </ol>
 */
@Slf4j
@Component
public class EmailCodeSignInOrRegisterStrategy
        extends AbstractSignInOrRegisterStrategy<EmailCodeSignInOrRegisterRequest> {

    public EmailCodeSignInOrRegisterStrategy(
            MemberFacade memberFacade,
            LoginService loginService,
            RegisterService registerService,
            VerificationCodeAdapter verificationCodeAdapter) {
        super(memberFacade, loginService, registerService, verificationCodeAdapter);
    }

    @Override
    public SignInOrRegisterType getType() {
        return SignInOrRegisterType.EMAIL_CODE;
    }

    @Override
    protected void validateCredentials(EmailCodeSignInOrRegisterRequest request) {
        // 验证邮箱验证码
        log.debug("验证邮箱验证码: email={}", request.getEmail());

        boolean isValid = verificationCodeAdapter.verifyAndDelete(
                request.getEmail(),
                request.getCode(),
                VerificationBizType.SIGN_IN_OR_REGISTER
        );

        if (!isValid) {
            throw new BusinessException("验证码无效或已过期");
        }

        log.debug("邮箱验证码验证成功: email={}", request.getEmail());
    }

    @Override
    protected String extractUniqueIdentifier(EmailCodeSignInOrRegisterRequest request) {
        return request.getEmail();
    }

    @Override
    protected boolean checkUserExists(String email) {
        R<Boolean> result = memberFacade.checkEmailExists(email);

        if (!result.isSuccess()) {
            throw new BusinessException("检查用户状态失败: " + result.getMsg());
        }

        return Boolean.TRUE.equals(result.getData());
    }

    @Override
    protected AuthResponse performLogin(EmailCodeSignInOrRegisterRequest request) {
        log.debug("构建邮箱验证码登录请求: email={}", request.getEmail());

        // 构造登录请求（复用现有 EmailCodeLoginStrategy）
        EmailCodeLoginRequest loginRequest = new EmailCodeLoginRequest();
        loginRequest.setEmail(request.getEmail());
        loginRequest.setCode(request.getCode());

        return loginService.login(loginRequest);
    }

    @Override
    protected AuthResponse performRegisterAndLogin(EmailCodeSignInOrRegisterRequest request) {
        log.debug("构建邮箱验证码注册请求: email={}", request.getEmail());

        // 构造注册请求（复用现有 EmailCodeRegisterStrategy）
        EmailCodeRegisterRequest registerRequest = new EmailCodeRegisterRequest();
        registerRequest.setEmail(request.getEmail());
        registerRequest.setCode(request.getCode());

        // 执行注册
        RegisterResponse registerResponse = registerService.register(registerRequest);

        // 提取认证响应
        return extractAuthResponse(registerResponse);
    }

    @Override
    protected void postProcess(EmailCodeSignInOrRegisterRequest request,
                               AuthResponse response,
                               boolean isNewUser) {
        if (isNewUser) {
            log.info("新用户通过邮箱验证码注册成功: email={}, memberId={}",
                    request.getEmail(),
                    response.getUserInfo() != null ? response.getUserInfo().getMemberId() : "unknown");

            // TODO: 可扩展：发送欢迎邮件、推送通知、赠送新人礼包等
        } else {
            log.info("用户通过邮箱验证码登录成功: email={}, memberId={}",
                    request.getEmail(),
                    response.getUserInfo() != null ? response.getUserInfo().getMemberId() : "unknown");
        }
    }
}


