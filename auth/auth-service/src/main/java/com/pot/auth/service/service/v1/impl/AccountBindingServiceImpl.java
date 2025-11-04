package com.pot.auth.service.service.v1.impl;

import com.pot.auth.service.dto.request.BindAccountRequest;
import com.pot.auth.service.dto.request.UnbindAccountRequest;
import com.pot.auth.service.dto.response.AccountBindingInfo;
import com.pot.auth.service.service.v1.AccountBindingService;
import com.pot.member.facade.api.SocialConnectionFacade;
import com.pot.member.facade.dto.SocialConnectionDTO;
import com.pot.member.facade.dto.request.BindSocialAccountRequest;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.model.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 账户绑定服务实现
 * <p>
 * 负责OAuth2流程编排和Member服务调用，遵循以下设计原则：
 * 1. 单一职责：只负责业务流程编排，不直接操作数据库
 * 2. 依赖倒置：依赖Facade抽象接口，而非具体实现
 * 3. 开闭原则：对扩展开放，对修改关闭
 * </p>
 *
 * @author Zing
 * @since 2025-11-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountBindingServiceImpl implements AccountBindingService {

    private final SocialConnectionFacade socialConnectionFacade;
    // private final OAuth2Service oauth2Service; // TODO: 注入OAuth2Service

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountBindingInfo bindAccount(Long userId, BindAccountRequest request) {
        String provider = request.getProvider();
        String code = request.getCode();
        String state = request.getState();

        log.info("[AccountBindingService] 绑定第三方账号, userId={}, provider={}", userId, provider);

        try {
            // 1. 验证state（CSRF防护）
            if (state != null && !state.isEmpty()) {
                validateState(state);
            }

            // 2. 使用code换取OAuth2用户信息
            // TODO: 集成OAuth2Service
            // OAuth2UserInfo userInfo = oauth2Service.getUserInfo(provider, code);

            // 模拟获取OAuth2用户信息（实际应从OAuth2Service获取）
            String providerMemberId = "oauth_" + provider + "_" + System.currentTimeMillis();
            String providerUsername = "User_" + providerMemberId.substring(providerMemberId.length() - 8);
            String accessToken = "access_token_" + System.currentTimeMillis();
            String refreshToken = "refresh_token_" + System.currentTimeMillis();
            Long expiresAt = System.currentTimeMillis() / 1000 + 7200; // 2小时后过期
            String avatarUrl = "https://avatars.example.com/default.jpg";

            // 3. 构建绑定请求
            BindSocialAccountRequest bindRequest = BindSocialAccountRequest.builder()
                    .memberId(userId)
                    .provider(provider.toLowerCase())
                    .providerMemberId(providerMemberId)
                    .providerUsername(providerUsername)
                    .providerEmail(null) // 可选
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenExpiresAt(expiresAt)
                    .scope("user:read,user:email")
                    .extendJson(buildExtendJson(avatarUrl))
                    .build();

            // 4. 调用Member Facade创建绑定
            R<SocialConnectionDTO> result = socialConnectionFacade.bindSocialAccount(bindRequest);

            if (!result.isSuccess() || result.getData() == null) {
                log.error("[AccountBindingService] 绑定失败, userId={}, provider={}, message={}",
                        userId, provider, result.getMsg());
                throw new BusinessException(result.getMsg() != null ? result.getMsg() : "绑定失败");
            }

            SocialConnectionDTO connectionDTO = result.getData();

            // 5. 转换为AccountBindingInfo
            AccountBindingInfo bindingInfo = convertToAccountBindingInfo(connectionDTO);

            log.info("[AccountBindingService] 绑定成功, userId={}, provider={}, connectionId={}",
                    userId, provider, connectionDTO.getId());

            return bindingInfo;

        } catch (BusinessException e) {
            log.warn("[AccountBindingService] 绑定业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[AccountBindingService] 绑定系统异常", e);
            throw new BusinessException("绑定失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindAccount(Long userId, UnbindAccountRequest request) {
        String provider = request.getProvider();

        log.info("[AccountBindingService] 解绑第三方账号, userId={}, provider={}", userId, provider);

        try {
            // 调用Member Facade解绑
            R<Void> result = socialConnectionFacade.unbindSocialAccount(userId, provider);

            if (!result.isSuccess()) {
                log.error("[AccountBindingService] 解绑失败, userId={}, provider={}, message={}",
                        userId, provider, result.getMsg());
                throw new BusinessException(result.getMsg() != null ? result.getMsg() : "解绑失败");
            }

            log.info("[AccountBindingService] 解绑成功, userId={}, provider={}", userId, provider);

        } catch (BusinessException e) {
            log.warn("[AccountBindingService] 解绑业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[AccountBindingService] 解绑系统异常", e);
            throw new BusinessException("解绑失败: " + e.getMessage());
        }
    }

    @Override
    public List<AccountBindingInfo> listBindings(Long userId) {
        log.debug("[AccountBindingService] 获取绑定列表, userId={}", userId);

        try {
            // 调用Member Facade查询
            R<List<SocialConnectionDTO>> result = socialConnectionFacade.getSocialConnections(userId);

            if (!result.isSuccess()) {
                log.error("[AccountBindingService] 查询绑定列表失败, userId={}, message={}",
                        userId, result.getMsg());
                return new ArrayList<>();
            }

            List<SocialConnectionDTO> connections = result.getData();
            if (connections == null) {
                return new ArrayList<>();
            }

            // 转换为AccountBindingInfo列表
            List<AccountBindingInfo> bindingInfos = connections.stream()
                    .map(this::convertToAccountBindingInfo)
                    .collect(Collectors.toList());

            log.debug("[AccountBindingService] 获取绑定列表成功, userId={}, count={}",
                    userId, bindingInfos.size());

            return bindingInfos;

        } catch (Exception e) {
            log.error("[AccountBindingService] 获取绑定列表异常, userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public AccountBindingInfo getBinding(Long userId, String provider) {
        log.debug("[AccountBindingService] 获取绑定信息, userId={}, provider={}", userId, provider);

        try {
            // 调用Member Facade查询
            R<SocialConnectionDTO> result = socialConnectionFacade.getSocialConnection(userId, provider);

            if (!result.isSuccess() || result.getData() == null) {
                log.warn("[AccountBindingService] 未找到绑定关系, userId={}, provider={}",
                        userId, provider);
                throw new BusinessException("未找到绑定关系");
            }

            SocialConnectionDTO connectionDTO = result.getData();
            AccountBindingInfo bindingInfo = convertToAccountBindingInfo(connectionDTO);

            log.debug("[AccountBindingService] 获取绑定信息成功, userId={}, provider={}",
                    userId, provider);

            return bindingInfo;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AccountBindingService] 获取绑定信息异常, userId={}, provider={}",
                    userId, provider, e);
            throw new BusinessException("获取绑定信息失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setPrimaryAccount(Long userId, String provider) {
        log.info("[AccountBindingService] 设置主账号, userId={}, provider={}", userId, provider);

        try {
            // 调用Member Facade设置主账号
            R<Void> result = socialConnectionFacade.setPrimarySocialAccount(userId, provider);

            if (!result.isSuccess()) {
                log.error("[AccountBindingService] 设置主账号失败, userId={}, provider={}, message={}",
                        userId, provider, result.getMsg());
                throw new BusinessException(result.getMsg() != null ? result.getMsg() : "设置主账号失败");
            }

            log.info("[AccountBindingService] 设置主账号成功, userId={}, provider={}", userId, provider);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AccountBindingService] 设置主账号异常", e);
            throw new BusinessException("设置主账号失败");
        }
    }

    @Override
    public boolean isAccountBound(String provider, String openId) {
        log.debug("[AccountBindingService] 检查绑定状态, provider={}, openId={}",
                provider, maskOpenId(openId));

        try {
            // 调用Member Facade检查
            R<Boolean> result = socialConnectionFacade.isSocialAccountBound(provider, openId);

            if (!result.isSuccess()) {
                log.warn("[AccountBindingService] 检查绑定状态失败, provider={}", provider);
                return false;
            }

            boolean isBound = result.getData() != null && result.getData();

            log.debug("[AccountBindingService] 检查绑定状态完成, provider={}, isBound={}",
                    provider, isBound);

            return isBound;

        } catch (Exception e) {
            log.error("[AccountBindingService] 检查绑定状态异常, provider={}", provider, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountBindingInfo refreshBinding(Long userId, String provider) {
        log.info("[AccountBindingService] 刷新绑定信息, userId={}, provider={}", userId, provider);

        try {
            // 1. 获取当前绑定信息
            AccountBindingInfo currentBinding = getBinding(userId, provider);

            // 2. 从OAuth提供商获取最新用户信息
            // TODO: 集成OAuth2Service
            // OAuth2UserInfo freshUserInfo = oauth2Service.refreshUserInfo(provider, currentBinding.getOpenId());

            // 3. 更新令牌（模拟）
            String newAccessToken = "new_access_token_" + System.currentTimeMillis();
            String newRefreshToken = "new_refresh_token_" + System.currentTimeMillis();
            Long newExpiresAt = System.currentTimeMillis() / 1000 + 7200;

            R<Void> updateResult = socialConnectionFacade.updateSocialAccountTokens(
                    userId, provider, newAccessToken, newRefreshToken, newExpiresAt);

            if (!updateResult.isSuccess()) {
                log.error("[AccountBindingService] 更新令牌失败, userId={}, provider={}",
                        userId, provider);
                throw new BusinessException("刷新失败");
            }

            // 4. 返回更新后的绑定信息
            AccountBindingInfo refreshedBinding = getBinding(userId, provider);

            log.info("[AccountBindingService] 刷新绑定信息成功, userId={}, provider={}",
                    userId, provider);

            return refreshedBinding;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AccountBindingService] 刷新绑定信息异常", e);
            throw new BusinessException("刷新绑定信息失败");
        }
    }

    /**
     * 验证CSRF state
     *
     * @param state CSRF token
     */
    private void validateState(String state) {
        // TODO: 从Redis或Session中验证state
        // 简化实现，实际应该验证state的有效性
        log.debug("[AccountBindingService] 验证state: {}", maskOpenId(state));
    }

    /**
     * 转换SocialConnectionDTO为AccountBindingInfo
     *
     * @param dto SocialConnectionDTO
     * @return AccountBindingInfo
     */
    private AccountBindingInfo convertToAccountBindingInfo(SocialConnectionDTO dto) {
        if (dto == null) {
            return null;
        }

        return AccountBindingInfo.builder()
                .bindingId(dto.getId())
                .provider(dto.getProvider())
                .openId(dto.getProviderMemberId())
                .nickname(dto.getProviderUsername())
                .avatarUrl(dto.getAvatarUrl())
                .isPrimary(dto.getIsPrimary() != null && dto.getIsPrimary())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .boundAt(dto.getBoundAt())
                .updatedAt(dto.getUpdatedAt())
                .lastUsedAt(dto.getLastUsedAt())
                .build();
    }

    /**
     * 构建扩展JSON
     *
     * @param avatarUrl 头像URL
     * @return JSON字符串
     */
    private String buildExtendJson(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return "{}";
        }
        // 简单的JSON构建，生产环境建议使用Jackson
        return String.format("{\"avatar\":\"%s\"}", avatarUrl);
    }

    /**
     * 脱敏OpenID（日志输出用）
     *
     * @param openId OpenID
     * @return 脱敏后的OpenID
     */
    private String maskOpenId(String openId) {
        if (openId == null || openId.length() < 8) {
            return "***";
        }
        return openId.substring(0, 4) + "***" + openId.substring(openId.length() - 4);
    }
}

