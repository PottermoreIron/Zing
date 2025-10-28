package com.pot.auth.service.service.v1.impl;

import com.pot.auth.service.dto.request.BindAccountRequest;
import com.pot.auth.service.dto.request.UnbindAccountRequest;
import com.pot.auth.service.dto.response.AccountBindingInfo;
import com.pot.auth.service.service.v1.AccountBindingService;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 账户绑定服务实现
 * <p>
 * 管理用户与第三方OAuth账号的绑定关系
 *
 * @author Zing
 * @since 2025-10-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountBindingServiceImpl implements AccountBindingService {

    // 这里需要注入实际的DAO和OAuth2Service
    // private final AccountBindingMapper accountBindingMapper;
    // private final OAuth2Service oauth2Service;

    /**
     * 绑定第三方账号
     */
    @Transactional(rollbackFor = Exception.class)
    public AccountBindingInfo bindAccount(Long userId, BindAccountRequest request) {
        String provider = request.getProvider();
        String code = request.getCode();
        String state = request.getState();

        log.info("[AccountBindingService] 绑定第三方账号, userId={}, provider={}", userId, provider);

        try {
            // 1. 验证state（CSRF防护）
            if (state != null) {
                validateState(state);
            }

            // 2. 使用code换取OAuth2用户信息
            // OAuth2UserInfo userInfo = oauth2Service.getUserInfo(provider, code);
            // 这里模拟获取到的用户信息
            String openId = "simulated_open_id_" + System.currentTimeMillis();
            String nickname = "ThirdPartyUser";
            String avatarUrl = "https://example.com/avatar.jpg";

            // 3. 检查该第三方账号是否已被其他用户绑定
            if (isAccountBoundByOther(provider, openId, userId)) {
                throw new BusinessException("该第三方账号已被其他用户绑定");
            }

            // 4. 检查当前用户是否已绑定该提供商的账号
            if (isUserBound(userId, provider)) {
                throw new BusinessException("您已绑定该平台的账号，请先解绑");
            }

            // 5. 创建绑定记录
            AccountBindingInfo bindingInfo = createBinding(userId, provider, openId, nickname, avatarUrl);

            log.info("[AccountBindingService] 绑定成功, userId={}, provider={}, openId={}",
                    userId, provider, maskOpenId(openId));

            return bindingInfo;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AccountBindingService] 绑定失败", e);
            throw new BusinessException("绑定失败: " + e.getMessage());
        }
    }

    /**
     * 解绑第三方账号
     */
    @Transactional(rollbackFor = Exception.class)
    public void unbindAccount(Long userId, UnbindAccountRequest request) {
        String provider = request.getProvider();

        log.info("[AccountBindingService] 解绑第三方账号, userId={}, provider={}", userId, provider);

        try {
            // 1. 检查绑定是否存在
            AccountBindingInfo binding = getBinding(userId, provider);
            if (binding == null) {
                throw new BusinessException("未找到绑定关系");
            }

            // 2. 检查是否至少保留一种登录方式
            if (!canUnbind(userId)) {
                throw new BusinessException("至少需要保留一种登录方式");
            }

            // 3. 删除绑定记录
            deleteBinding(userId, provider);

            log.info("[AccountBindingService] 解绑成功, userId={}, provider={}", userId, provider);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AccountBindingService] 解绑失败", e);
            throw new BusinessException("解绑失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的所有绑定
     */
    public List<AccountBindingInfo> listBindings(Long userId) {
        try {
            // 查询数据库获取绑定列表
            // List<AccountBindingInfo> bindings = accountBindingMapper.selectByUserId(userId);

            // 模拟返回
            List<AccountBindingInfo> bindings = new ArrayList<>();

            log.debug("[AccountBindingService] 获取绑定列表, userId={}, count={}",
                    userId, bindings.size());

            return bindings;

        } catch (Exception e) {
            log.error("[AccountBindingService] 获取绑定列表失败", e);
            throw new BusinessException("获取绑定列表失败");
        }
    }

    /**
     * 获取指定提供商的绑定信息
     */
    public AccountBindingInfo getBinding(Long userId, String provider) {
        try {
            // 从数据库查询
            // AccountBindingInfo binding = accountBindingMapper.selectByUserIdAndProvider(userId, provider);

            // 模拟实现
            AccountBindingInfo binding = null;

            if (binding == null) {
                throw new BusinessException("未找到绑定关系");
            }

            return binding;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AccountBindingService] 获取绑定信息失败", e);
            throw new BusinessException("获取绑定信息失败");
        }
    }

    /**
     * 设置主账号
     */
    @Transactional(rollbackFor = Exception.class)
    public void setPrimaryAccount(Long userId, String provider) {
        try {
            // 1. 检查绑定是否存在
            AccountBindingInfo binding = getBinding(userId, provider);
            if (binding == null) {
                throw new BusinessException("未找到绑定关系");
            }

            // 2. 取消其他主账号
            // accountBindingMapper.clearPrimary(userId);

            // 3. 设置为主账号
            // accountBindingMapper.setPrimary(binding.getBindingId());

            log.info("[AccountBindingService] 设置主账号成功, userId={}, provider={}", userId, provider);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AccountBindingService] 设置主账号失败", e);
            throw new BusinessException("设置主账号失败");
        }
    }

    /**
     * 检查第三方账号是否已被绑定
     */
    public boolean isAccountBound(String provider, String openId) {
        try {
            // 查询数据库
            // AccountBindingInfo binding = accountBindingMapper.selectByProviderAndOpenId(provider, openId);
            // return binding != null;

            return false; // 模拟实现

        } catch (Exception e) {
            log.error("[AccountBindingService] 检查绑定状态失败", e);
            return false;
        }
    }

    /**
     * 刷新绑定信息
     */
    @Transactional(rollbackFor = Exception.class)
    public AccountBindingInfo refreshBinding(Long userId, String provider) {
        try {
            // 1. 检查绑定是否存在
            AccountBindingInfo binding = getBinding(userId, provider);
            if (binding == null) {
                throw new BusinessException("未找到绑定关系");
            }

            // 2. 从OAuth提供商获取最新用户信息
            // OAuth2UserInfo userInfo = oauth2Service.refreshUserInfo(provider, binding.getOpenId());

            // 3. 更新绑定信息
            // binding.setNickname(userInfo.getNickname());
            // binding.setAvatarUrl(userInfo.getAvatarUrl());
            // binding.setUpdatedAt(LocalDateTime.now());
            // accountBindingMapper.update(binding);

            log.info("[AccountBindingService] 刷新绑定信息成功, userId={}, provider={}", userId, provider);

            return binding;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AccountBindingService] 刷新绑定信息失败", e);
            throw new BusinessException("刷新绑定信息失败");
        }
    }

    /**
     * 验证CSRF state
     */
    private void validateState(String state) {
        // 从Redis或Session中验证state
        // 简化实现，这里不做具体验证
    }

    /**
     * 检查第三方账号是否已被其他用户绑定
     */
    private boolean isAccountBoundByOther(String provider, String openId, Long currentUserId) {
        // 查询数据库检查
        // AccountBindingInfo binding = accountBindingMapper.selectByProviderAndOpenId(provider, openId);
        // return binding != null && !binding.getUserId().equals(currentUserId);

        return false; // 模拟实现
    }

    /**
     * 检查用户是否已绑定该提供商
     */
    private boolean isUserBound(Long userId, String provider) {
        // 查询数据库检查
        // AccountBindingInfo binding = accountBindingMapper.selectByUserIdAndProvider(userId, provider);
        // return binding != null;

        return false; // 模拟实现
    }

    /**
     * 创建绑定记录
     */
    private AccountBindingInfo createBinding(Long userId, String provider, String openId,
                                             String nickname, String avatarUrl) {
        Long now = TimeUtils.currentTimestamp();

        AccountBindingInfo binding = AccountBindingInfo.builder()
                .bindingId(System.currentTimeMillis()) // 模拟ID
                .userId(userId)
                .provider(provider)
                .openId(openId)
                .nickname(nickname)
                .avatarUrl(avatarUrl)
                .isPrimary(false)
                .status("ACTIVE")
                .boundAt(now)
                .updatedAt(now)
                .lastUsedAt(now)
                .build();

        // 保存到数据库
        // accountBindingMapper.insert(binding);

        return binding;
    }

    /**
     * 检查是否可以解绑（至少保留一种登录方式）
     */
    private boolean canUnbind(Long userId) {
        // 检查用户的登录方式数量
        // 1. 统计绑定的第三方账号数量
        // 2. 检查是否设置了密码
        // 3. 检查是否绑定了手机号/邮箱

        // 简化实现：假设可以解绑
        return true;
    }

    /**
     * 删除绑定记录
     */
    private void deleteBinding(Long userId, String provider) {
        // 从数据库删除
        // accountBindingMapper.deleteByUserIdAndProvider(userId, provider);
    }

    /**
     * 脱敏OpenID
     */
    private String maskOpenId(String openId) {
        if (openId == null || openId.length() < 8) {
            return "***";
        }
        return openId.substring(0, 4) + "***" + openId.substring(openId.length() - 4);
    }
}

