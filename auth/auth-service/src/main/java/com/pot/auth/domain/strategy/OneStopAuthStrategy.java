package com.pot.auth.domain.strategy;

import com.pot.auth.domain.authentication.entity.AuthenticationResult;
import com.pot.auth.domain.context.OneStopAuthContext;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.interfaces.dto.onestop.OneStopAuthRequest;

/**
 * 一键认证策略接口
 *
 * <p>
 * 定义一键认证（自动注册/登录）策略的核心方法
 *
 * <p>
 * <strong>设计理念：</strong>
 * <ul>
 * <li>用户无需选择注册或登录</li>
 * <li>系统自动判断用户是否存在</li>
 * <li>已存在 → 执行登录逻辑</li>
 * <li>不存在 → 执行注册逻辑（自动生成默认信息）</li>
 * </ul>
 *
 * <p>
 * <strong>与传统策略的区别：</strong>
 * <table border="1">
 * <tr>
 * <th>策略类型</th>
 * <th>用户存在时</th>
 * <th>用户不存在时</th>
 * </tr>
 * <tr>
 * <td>LoginStrategy</td>
 * <td>✅ 登录</td>
 * <td>❌ 报错</td>
 * </tr>
 * <tr>
 * <td>RegisterStrategy</td>
 * <td>❌ 报错</td>
 * <td>✅ 注册</td>
 * </tr>
 * <tr>
 * <td>OneStopAuthStrategy</td>
 * <td>✅ 登录</td>
 * <td>✅ 注册</td>
 * </tr>
 * </table>
 *
 * @param <T> 具体的认证请求类型，必须继承自 OneStopAuthRequest
 * @author pot
 * @since 2025-11-29
 */
public interface OneStopAuthStrategy<T extends OneStopAuthRequest> {

    /**
     * 执行一键认证逻辑
     *
     * <p>
     * 执行流程：
     * <ol>
     * <li>基础校验（参数、格式等）</li>
     * <li>查找用户</li>
     * <li>用户已存在 → 验证凭证 → 登录</li>
     * <li>用户不存在 → 验证凭证 → 注册 → 登录</li>
     * <li>生成Token</li>
     * </ol>
     *
     * @param context 认证上下文（包含请求、IP、设备信息等）
     * @return 认证结果（包含Token）
     */
    AuthenticationResult execute(OneStopAuthContext context);

    /**
     * 判断该策略是否支持指定的认证类型
     *
     * @param authType 认证类型
     * @return true if支持, false otherwise
     */
    boolean supports(AuthType authType);

    /**
     * 获取该策略支持的认证类型
     *
     * @return 认证类型
     */
    AuthType getSupportedAuthType();
}
