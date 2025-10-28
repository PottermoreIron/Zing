package com.pot.auth.service.strategy.impl.login;

import com.pot.auth.service.dto.oauth2.OAuth2TokenResponse;
import com.pot.auth.service.dto.oauth2.OAuth2UserInfo;
import com.pot.auth.service.dto.request.login.OAuth2LoginRequest;
import com.pot.auth.service.enums.OAuth2Provider;
import com.pot.auth.service.oauth2.OAuth2ClientService;
import com.pot.auth.service.oauth2.factory.OAuth2ClientFactory;
import com.pot.auth.service.service.adapter.VerificationCodeAdapter;
import com.pot.auth.service.strategy.impl.LoginStrategy;
import com.pot.auth.service.utils.UserTokenUtils;
import com.pot.member.facade.api.MemberFacade;
import com.pot.member.facade.dto.MemberDTO;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;

/**
 * @author: Pot
 * @created: 2025/10/22
 * @description: OAuth2登录策略抽象基类 - 实现OAuth2登录的通用流程
 */
@Slf4j
public abstract class AbstractOAuth2LoginStrategy extends AbstractLoginStrategyImpl<OAuth2LoginRequest>
        implements LoginStrategy<OAuth2LoginRequest> {

    private static final String OAUTH2_STATE_PREFIX = "oauth2:state:";
    private static final String OAUTH2_BIND_PREFIX = "oauth2:bind:";
    // state参数10分钟过期
    private static final Duration STATE_EXPIRE_DURATION = Duration.ofMinutes(10);
    protected final OAuth2ClientFactory oauth2ClientFactory;
    protected final RedisService redisService;

    public AbstractOAuth2LoginStrategy(
            MemberFacade memberFacade,
            UserTokenUtils userTokenUtils,
            VerificationCodeAdapter verificationCodeAdapter,
            OAuth2ClientFactory oauth2ClientFactory,
            RedisService redisService) {
        super(memberFacade, userTokenUtils, verificationCodeAdapter);
        this.oauth2ClientFactory = oauth2ClientFactory;
        this.redisService = redisService;
    }

    /**
     * 获取OAuth2提供商
     */
    protected abstract OAuth2Provider getOAuth2Provider();

    @Override
    protected void validateBusinessRules(OAuth2LoginRequest request) {
        // 1. 验证state参数，防止CSRF攻击
        String stateKey = OAUTH2_STATE_PREFIX + request.getState();
        String storedSessionId = redisService.get(stateKey, String.class);

        if (StringUtils.isBlank(storedSessionId)) {
            throw new BusinessException("无效的state参数，可能是CSRF攻击");
        }

        // todo 验证state与当前会话的关联
        String currentSessionId = "";
        if (!storedSessionId.equals(currentSessionId)) {
            throw new BusinessException("state参数会话不匹配");
        }


        // 验证通过后删除state
        redisService.delete(stateKey);

        log.debug("OAuth2登录业务规则校验通过: provider={}", getOAuth2Provider().getProvider());
    }

    @Override
    protected MemberDTO getMember(OAuth2LoginRequest request) {
        // 1. 获取OAuth2客户端服务
        OAuth2ClientService oauth2Client = oauth2ClientFactory.getClientService(getOAuth2Provider());

        // 2. 使用授权码换取访问令牌
        OAuth2TokenResponse tokenResponse = oauth2Client.exchangeToken(request.getCode());

        // 3. 使用访问令牌获取用户信息
        OAuth2UserInfo oauth2UserInfo = oauth2Client.getUserInfo(tokenResponse.getAccessToken());

        // 4. 查询系统中是否已存在该OAuth2账号绑定的用户
        R<MemberDTO> result = memberFacade.getMemberByOAuth2(
                getOAuth2Provider().getProvider(),
                oauth2UserInfo.getOpenId()
        );

        MemberDTO memberDTO;

        if (result.isSuccess() && result.getData() != null) {
            // 已绑定用户，直接返回
            memberDTO = result.getData();
            log.info("OAuth2账号已绑定: provider={}, openId={}, memberId={}",
                    getOAuth2Provider().getProvider(),
                    oauth2UserInfo.getOpenId(),
                    memberDTO.getMemberId());
        } else {
            // 未绑定用户，创建新用户或提示绑定
            memberDTO = handleUnboundOAuth2Account(oauth2UserInfo);
        }

        // 5. 缓存OAuth2用户信息，用于后续可能的绑定操作
        cacheOAuth2UserInfo(memberDTO.getMemberId(), oauth2UserInfo);

        return memberDTO;
    }

    @Override
    protected void validateCredentials(OAuth2LoginRequest request, MemberDTO memberDTO) {
        // OAuth2登录不需要验证密码
        log.debug("OAuth2登录无需验证密码");
    }

    /**
     * 处理未绑定的OAuth2账号
     *
     * @param oauth2UserInfo OAuth2用户信息
     * @return MemberDTO
     */
    protected MemberDTO handleUnboundOAuth2Account(OAuth2UserInfo oauth2UserInfo) {
        log.info("OAuth2账号未绑定，自动创建新用户: provider={}, openId={}",
                getOAuth2Provider().getProvider(),
                oauth2UserInfo.getOpenId());

        // 自动创建新用户
        R<MemberDTO> createResult = memberFacade.createMemberFromOAuth2(
                getOAuth2Provider().getProvider(),
                oauth2UserInfo.getOpenId(),
                oauth2UserInfo.getEmail(),
                oauth2UserInfo.getNickname(),
                oauth2UserInfo.getAvatarUrl()
        );

        if (!createResult.isSuccess() || createResult.getData() == null) {
            throw new BusinessException("OAuth2登录失败：创建用户失败");
        }

        return createResult.getData();
    }

    /**
     * 缓存OAuth2用户信息
     */
    protected void cacheOAuth2UserInfo(Long memberId, OAuth2UserInfo oauth2UserInfo) {
        try {
            String key = OAUTH2_BIND_PREFIX + memberId;
            // 这里可以将OAuth2UserInfo序列化后存储，便于后续操作
            redisService.set(key, oauth2UserInfo.getOpenId(), Duration.ofHours(1));
        } catch (Exception e) {
            log.error("缓存OAuth2用户信息失败", e);
            // 不影响登录流程
        }
    }

    /**
     * 生成并缓存state参数
     */
    public String generateAndCacheState() {
        String state = java.util.UUID.randomUUID().toString();
        String stateKey = OAUTH2_STATE_PREFIX + state;
        redisService.set(stateKey, "1", STATE_EXPIRE_DURATION);
        return state;
    }
}