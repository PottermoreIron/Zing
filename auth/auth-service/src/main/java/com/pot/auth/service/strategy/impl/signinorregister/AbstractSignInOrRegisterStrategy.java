package com.pot.auth.service.strategy.impl.signinorregister;

import com.pot.auth.service.dto.request.signinorregister.SignInOrRegisterRequest;
import com.pot.auth.service.dto.response.AuthResponse;
import com.pot.auth.service.dto.response.RegisterResponse;
import com.pot.auth.service.enums.SignInOrRegisterType;
import com.pot.auth.service.service.LoginService;
import com.pot.auth.service.service.RegisterService;
import com.pot.auth.service.service.adapter.VerificationCodeAdapter;
import com.pot.auth.service.strategy.SignInOrRegisterStrategy;
import com.pot.member.facade.api.MemberFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: Pot
 * @created: 2025/10/23
 * @description: 一键登录/注册抽象策略实现 - 模板方法模式
 *
 * <p>核心设计思想：</p>
 * <ul>
 *   <li>定义统一的业务流程骨架</li>
 *   <li>复用现有的 Login 和 Register 服务</li>
 *   <li>子类只需实现差异化的部分</li>
 *   <li>保证流程一致性和代码复用</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractSignInOrRegisterStrategy<T extends SignInOrRegisterRequest>
        implements SignInOrRegisterStrategy<T> {

    protected final MemberFacade memberFacade;
    protected final LoginService loginService;
    protected final RegisterService registerService;
    protected final VerificationCodeAdapter verificationCodeAdapter;

    @Override
    public AuthResponse signInOrRegister(T request) {
        log.info("开始一键登录/注册流程: type={}", request.getType());

        // 1. 前置校验（参数格式等由 @Valid 完成）
        preValidate(request);

        // 2. 验证凭证（验证码/OAuth2 Token）
        validateCredentials(request);

        // 3. 获取用户唯一标识（手机号/邮箱/OAuth2 openId）
        String uniqueIdentifier = extractUniqueIdentifier(request);
        log.debug("提取用户唯一标识: identifier={}", uniqueIdentifier);

        // 4. 检查用户是否存在
        boolean userExists = checkUserExists(uniqueIdentifier);
        log.debug("用户存在性检查: identifier={}, exists={}", uniqueIdentifier, userExists);

        AuthResponse response;
        if (userExists) {
            // 5a. 用户已注册 → 直接登录
            log.info("用户已存在，执行登录流程: identifier={}", uniqueIdentifier);
            response = performLogin(request);
        } else {
            // 5b. 用户未注册 → 注册后登录
            log.info("用户不存在，执行注册+登录流程: identifier={}", uniqueIdentifier);
            response = performRegisterAndLogin(request);
        }

        // 6. 后置处理（记录日志、发送通知等）
        postProcess(request, response, userExists);

        log.info("一键登录/注册流程完成: identifier={}, isNewUser={}, memberId={}",
                uniqueIdentifier,
                !userExists,
                response.getUserInfo() != null ? response.getUserInfo().getMemberId() : "unknown");

        return response;
    }

    /**
     * 获取策略支持的认证类型（子类实现）
     */
    @Override
    public abstract SignInOrRegisterType getType();

    /**
     * 前置校验（子类可选实现）
     *
     * @param request 请求对象
     */
    protected void preValidate(T request) {
        // 默认不做额外校验，子类可覆盖
    }

    /**
     * 验证凭证（验证码、OAuth2 Token等）
     *
     * @param request 请求对象
     */
    protected abstract void validateCredentials(T request);

    /**
     * 提取用户唯一标识
     *
     * @param request 请求对象
     * @return 唯一标识（手机号/邮箱/OAuth2 openId）
     */
    protected abstract String extractUniqueIdentifier(T request);

    /**
     * 检查用户是否存在
     *
     * @param uniqueIdentifier 用户唯一标识
     * @return true-用户存在，false-用户不存在
     */
    protected abstract boolean checkUserExists(String uniqueIdentifier);

    /**
     * 执行登录（复用现有 LoginService）
     *
     * @param request 请求对象
     * @return AuthResponse
     */
    protected abstract AuthResponse performLogin(T request);

    /**
     * 执行注册并登录（复用现有 RegisterService）
     *
     * @param request 请求对象
     * @return AuthResponse
     */
    protected abstract AuthResponse performRegisterAndLogin(T request);

    /**
     * 后置处理（可选：记录日志、发送欢迎消息等）
     *
     * @param request   请求对象
     * @param response  响应对象
     * @param isNewUser 是否为新用户
     */
    protected void postProcess(T request, AuthResponse response, boolean isNewUser) {
        // 默认不做后置处理，子类可覆盖
        if (isNewUser) {
            log.info("新用户首次登录完成: memberId={}",
                    response.getUserInfo() != null ? response.getUserInfo().getMemberId() : "unknown");
        }
    }

    /**
     * 从注册响应中提取认证响应
     *
     * @param registerResponse 注册响应
     * @return AuthResponse
     */
    protected AuthResponse extractAuthResponse(RegisterResponse registerResponse) {
        if (registerResponse == null) {
            throw new IllegalStateException("注册响应不能为空");
        }
        AuthResponse authResponse = registerResponse.getAuthResponse();
        if (authResponse == null) {
            throw new IllegalStateException("注册响应中的认证信息不能为空");
        }
        return authResponse;
    }
}

