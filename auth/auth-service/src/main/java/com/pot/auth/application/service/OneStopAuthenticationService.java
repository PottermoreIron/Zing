package com.pot.auth.application.service;

import com.pot.auth.application.dto.OneStopAuthResponse;
import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.context.OneStopAuthContext;
import com.pot.auth.domain.shared.valueobject.DeviceInfo;
import com.pot.auth.domain.shared.valueobject.IpAddress;
import com.pot.auth.domain.strategy.OneStopAuthStrategy;
import com.pot.auth.domain.strategy.factory.OneStopAuthStrategyFactory;
import com.pot.auth.interfaces.dto.onestop.OneStopAuthRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 一键认证应用服务
 *
 * <p>
 * 提供一键认证（自动注册/登录）的业务逻辑
 *
 * <p>
 * <strong>职责：</strong>
 * <ul>
 * <li>构建认证上下文</li>
 * <li>选择合适的策略</li>
 * <li>执行认证流程</li>
 * <li>转换响应</li>
 * </ul>
 *
 * @author pot
 * @since 2025-11-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OneStopAuthenticationService {

    private final OneStopAuthStrategyFactory strategyFactory;

    /**
     * 一键认证（自动处理注册/登录）
     *
     * @param request   认证请求
     * @param ipAddress IP地址
     * @param userAgent User-Agent
     * @return 认证响应
     */
    public OneStopAuthResponse authenticate(
            OneStopAuthRequest request,
            String ipAddress,
            String userAgent) {

        log.info("[一键认证服务] 开始处理认证请求: authType={}, userDomain={}",
                request.authType(), request.userDomain());

        // 1. 构建认证上下文
        OneStopAuthContext context = OneStopAuthContext.builder()
                .request(request)
                .ipAddress(IpAddress.of(ipAddress))
                .deviceInfo(DeviceInfo.fromUserAgent(userAgent != null ? userAgent : "Unknown"))
                .build();

        // 2. 获取策略并执行
        @SuppressWarnings("unchecked")
        OneStopAuthStrategy<OneStopAuthRequest> strategy = (OneStopAuthStrategy<OneStopAuthRequest>) strategyFactory
                .getStrategy(request.authType());
        AuthenticationResult result = strategy.execute(context);

        // 3. 转换为响应
        OneStopAuthResponse response = OneStopAuthResponse.from(result);

        log.info("[一键认证服务] 认证成功: userId={}, authType={}",
                result.userId(), request.authType());

        return response;
    }
}
