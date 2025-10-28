package com.pot.auth.service.service.v1;

import com.pot.auth.service.dto.request.BindAccountRequest;
import com.pot.auth.service.dto.request.UnbindAccountRequest;
import com.pot.auth.service.dto.response.AccountBindingInfo;

import java.util.List;

/**
 * 账户绑定服务接口
 * <p>
 * 管理用户与第三方OAuth账号的绑定关系
 *
 * @author Zing
 * @since 2025-10-26
 */
public interface AccountBindingService {

    /**
     * 绑定第三方账号
     *
     * @param userId  用户ID
     * @param request 绑定请求
     * @return 绑定信息
     */
    AccountBindingInfo bindAccount(Long userId, BindAccountRequest request);

    /**
     * 解绑第三方账号
     *
     * @param userId  用户ID
     * @param request 解绑请求
     */
    void unbindAccount(Long userId, UnbindAccountRequest request);

    /**
     * 获取用户的所有绑定
     *
     * @param userId 用户ID
     * @return 绑定列表
     */
    List<AccountBindingInfo> listBindings(Long userId);

    /**
     * 获取指定提供商的绑定信息
     *
     * @param userId   用户ID
     * @param provider OAuth提供商
     * @return 绑定信息
     */
    AccountBindingInfo getBinding(Long userId, String provider);

    /**
     * 设置主账号
     *
     * @param userId   用户ID
     * @param provider OAuth提供商
     */
    void setPrimaryAccount(Long userId, String provider);

    /**
     * 检查第三方账号是否已被绑定
     *
     * @param provider OAuth提供商
     * @param openId   第三方平台的用户ID
     * @return 是否已绑定
     */
    boolean isAccountBound(String provider, String openId);

    /**
     * 刷新绑定信息
     *
     * @param userId   用户ID
     * @param provider OAuth提供商
     * @return 更新后的绑定信息
     */
    AccountBindingInfo refreshBinding(Long userId, String provider);
}

