package com.pot.auth.interfaces.controller;

import com.pot.auth.application.dto.OneStopAuthResponse;
import com.pot.auth.application.service.OneStopAuthenticationService;
import com.pot.auth.interfaces.dto.onestop.OneStopAuthRequest;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.ratelimit.annotation.RateLimit;
import com.pot.zing.framework.starter.ratelimit.enums.RateLimitMethodEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.pot.zing.framework.common.util.IpUtils.getClientIp;

/**
 * 认证控制器
 *
 * <p>
 * 负责所有一体化认证（自动注册/登录）
 *
 * <p>
 * 设计理念：
 * <ul>
 * <li>用户无需选择注册或登录</li>
 * <li>系统自动判断用户是否存在</li>
 * <li>用户不存在 → 自动注册</li>
 * <li>用户已存在 → 直接登录</li>
 * <li>提供无缝的用户体验</li>
 * </ul>
 *
 * <p>
 * 支持的认证方式：
 * <ul>
 * <li>用户名 + 密码</li>
 * <li>手机号 + 密码</li>
 * <li>手机号 + 验证码</li>
 * <li>邮箱 + 密码</li>
 * <li>邮箱 + 验证码</li>
 * <li>OAuth2（Google、GitHub、Facebook、Apple、Microsoft）</li>
 * <li>微信</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-30
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class OneStopAuthenticationController {

        private final OneStopAuthenticationService oneStopAuthenticationService;

        /**
         * 统一一键认证入口
         *
         * <p>
         * 支持所有一键认证方式，通过 authType 字段自动识别：
         * <ul>
         * <li>USERNAME_PASSWORD - 用户名密码认证</li>
         * <li>PHONE_PASSWORD - 手机号密码认证</li>
         * <li>PHONE_CODE - 手机号验证码认证</li>
         * <li>EMAIL_PASSWORD - 邮箱密码认证</li>
         * <li>EMAIL_CODE - 邮箱验证码认证</li>
         * <li>OAUTH2 - OAuth2认证（Google/GitHub/Facebook/Apple/Microsoft）</li>
         * <li>WECHAT - 微信认证</li>
         * </ul>
         *
         * <p>
         * 请求示例（手机号验证码）：
         *
         * <pre>
         * POST /auth/api/v1/authenticate
         * {
         *   "authType": "PHONE_CODE",
         *   "phone": "13800138000",
         *   "code": "123456",
         *   "userDomain": "MEMBER"
         * }
         * </pre>
         *
         * <p>
         * 请求示例（OAuth2）：
         *
         * <pre>
         * POST /auth/api/v1/authenticate
         * {
         *   "authType": "OAUTH2",
         *   "provider": "GOOGLE",
         *   "code": "4/0AY0e-g7...",
         *   "state": "random_state",
         *   "userDomain": "MEMBER"
         * }
         * </pre>
         *
         * <p>
         * 请求示例（微信）：
         *
         * <pre>
         * POST /auth/api/v1/authenticate
         * {
         *   "authType": "WECHAT",
         *   "code": "071abc123",
         *   "state": "STATE",
         *   "userDomain": "MEMBER"
         * }
         * </pre>
         */
        @RateLimit(type = RateLimitMethodEnum.IP_BASED, rate = 5.0, message = "认证请求过于频繁，请稍后再试")
        @PostMapping("/api/v1/authenticate")
        public R<OneStopAuthResponse> authenticate(
                        @Valid @RequestBody OneStopAuthRequest request,
                        HttpServletRequest httpRequest) {

                log.info("[一键认证] 收到认证请求: authType={}, userDomain={}",
                                request.authType(), request.userDomain());

                OneStopAuthResponse response = oneStopAuthenticationService.authenticate(
                                request,
                                getClientIp(httpRequest),
                                httpRequest.getHeader("User-Agent"));

                log.info("[一键认证] 认证成功: userId={}", response.userId());

                return R.success(response);
        }
}
