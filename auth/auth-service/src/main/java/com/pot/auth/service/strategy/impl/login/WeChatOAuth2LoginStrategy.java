package com.pot.auth.service.strategy.impl.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pot.auth.service.dto.oauth2.OAuth2TokenResponse;
import com.pot.auth.service.dto.oauth2.OAuth2UserInfo;
import com.pot.auth.service.enums.LoginType;
import com.pot.auth.service.enums.OAuth2Provider;
import com.pot.auth.service.oauth2.factory.OAuth2ClientFactory;
import com.pot.auth.service.service.adapter.VerificationCodeAdapter;
import com.pot.member.facade.api.MemberFacade;
import com.pot.member.facade.dto.MemberDTO;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.starter.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: Pot
 * @created: 2025/10/23
 * @description: 微信OAuth2登录策略
 * <p>
 * 微信登录特点：
 * 1. 支持unionId账号统一（同一开放平台下的多个应用）
 * 2. 提供丰富的用户信息（性别、地区等）
 * 3. 支持扫码登录、公众号登录、小程序登录等多种方式
 */
@Slf4j
@Service
public class WeChatOAuth2LoginStrategy extends AbstractOAuth2LoginStrategy {

    private final ObjectMapper objectMapper;

    public WeChatOAuth2LoginStrategy(
            MemberFacade memberFacade,
            com.pot.auth.service.utils.UserTokenUtils userTokenUtils,
            VerificationCodeAdapter verificationCodeAdapter,
            OAuth2ClientFactory oauth2ClientFactory,
            RedisService redisService,
            ObjectMapper objectMapper) {
        super(memberFacade, userTokenUtils, verificationCodeAdapter, oauth2ClientFactory, redisService);
        this.objectMapper = objectMapper;
    }

    @Override
    protected OAuth2Provider getOAuth2Provider() {
        return OAuth2Provider.WECHAT;
    }

    @Override
    public LoginType getLoginType() {
        return LoginType.OAUTH2_WECHAT;
    }

    @Override
    protected MemberDTO handleUnboundOAuth2Account(OAuth2UserInfo oauth2UserInfo) {
        log.info("处理未绑定的微信账号: openId={}, unionId={}",
                oauth2UserInfo.getOpenId(), oauth2UserInfo.getUnionId());

        // 微信特有：如果存在unionId，尝试通过unionId查找已有账号
        if (oauth2UserInfo.getUnionId() != null && !oauth2UserInfo.getUnionId().isEmpty()) {
            R<MemberDTO> result = memberFacade.getMemberByUnionId(oauth2UserInfo.getUnionId());

            if (result.isSuccess() && result.getData() != null) {
                MemberDTO memberDTO = result.getData();
                log.info("通过unionId找到已有账号: unionId={}, memberId={}",
                        oauth2UserInfo.getUnionId(), memberDTO.getMemberId());

                // 绑定当前openId到该账号
                bindWeChatOpenId(memberDTO.getMemberId(), oauth2UserInfo);

                return memberDTO;
            }
        }

        // 创建新账号
        return createNewMemberFromWeChat(oauth2UserInfo);
    }

    /**
     * 从微信信息创建新用户
     */
    private MemberDTO createNewMemberFromWeChat(OAuth2UserInfo oauth2UserInfo) {
        log.info("创建微信新用户: openId={}, nickname={}",
                oauth2UserInfo.getOpenId(), oauth2UserInfo.getNickname());

        R<MemberDTO> createResult = memberFacade.createMemberFromOAuth2(
                getOAuth2Provider().getProvider(),
                oauth2UserInfo.getOpenId(),
                oauth2UserInfo.getEmail(),
                oauth2UserInfo.getNickname(),
                oauth2UserInfo.getAvatarUrl()
        );

        if (!createResult.isSuccess() || createResult.getData() == null) {
            throw new BusinessException("微信登录失败：创建用户失败 - " + createResult.getMsg());
        }

        MemberDTO memberDTO = createResult.getData();

        // 如果有unionId，更新到用户信息
        if (oauth2UserInfo.getUnionId() != null) {
            updateUnionId(memberDTO.getMemberId(), oauth2UserInfo.getUnionId());
        }

        // 更新地区信息（微信特有）
        updateWeChatUserInfo(memberDTO.getMemberId(), oauth2UserInfo);

        return memberDTO;
    }

    /**
     * 绑定微信openId到已有账号
     */
    private void bindWeChatOpenId(Long memberId, OAuth2UserInfo oauth2UserInfo) {
        try {
            R<Void> result = memberFacade.bindOAuth2Account(
                    memberId,
                    getOAuth2Provider().getProvider(),
                    oauth2UserInfo.getOpenId()
            );

            if (!result.isSuccess()) {
                log.error("绑定微信openId失败: memberId={}, openId={}",
                        memberId, oauth2UserInfo.getOpenId());
            } else {
                log.info("绑定微信openId成功: memberId={}, openId={}",
                        memberId, oauth2UserInfo.getOpenId());
            }
        } catch (Exception e) {
            log.error("绑定微信openId异常", e);
        }
    }

    /**
     * 更新用户的unionId
     */
    private void updateUnionId(Long memberId, String unionId) {
        try {
            R<Void> result = memberFacade.updateUnionId(memberId, unionId);
            if (!result.isSuccess()) {
                log.error("更新unionId失败: memberId={}, unionId={}", memberId, unionId);
            } else {
                log.info("更新unionId成功: memberId={}, unionId={}", memberId, unionId);
            }
        } catch (Exception e) {
            log.error("更新unionId异常", e);
        }
    }

    /**
     * 更新微信用户信息（地区、性别等）
     */
    private void updateWeChatUserInfo(Long memberId, OAuth2UserInfo oauth2UserInfo) {
        try {
            // 这里可以调用member服务更新用户的详细信息
            log.debug("更新微信用户信息: memberId={}, country={}, province={}, city={}, gender={}",
                    memberId, oauth2UserInfo.getCountry(),
                    oauth2UserInfo.getProvince(), oauth2UserInfo.getCity(),
                    oauth2UserInfo.getGender());

            // 实际调用member服务的接口
            // memberFacade.updateWeChatUserInfo(memberId, oauth2UserInfo);
        } catch (Exception e) {
            log.error("更新微信用户信息异常", e);
        }
    }

    @Override
    protected MemberDTO getMember(com.pot.auth.service.dto.request.login.OAuth2LoginRequest request) {
        // 重写getMember以处理微信的tokenResponse传递问题
        var oauth2Client = oauth2ClientFactory.getClientService(getOAuth2Provider());

        // 1. 使用授权码换取访问令牌
        OAuth2TokenResponse tokenResponse = oauth2Client.exchangeToken(request.getCode());

        // 2. 获取用户信息（传递完整的tokenResponse序列化后的字符串）
        OAuth2UserInfo oauth2UserInfo;
        try {
            String tokenResponseJson = objectMapper.writeValueAsString(tokenResponse);
            oauth2UserInfo = oauth2Client.getUserInfo(tokenResponseJson);
        } catch (Exception e) {
            log.error("序列化tokenResponse失败，降级使用accessToken", e);
            oauth2UserInfo = oauth2Client.getUserInfo(tokenResponse.getAccessToken());
        }

        // 3. 查询系统中是否已存在该OAuth2账号绑定的用户
        R<MemberDTO> result = memberFacade.getMemberByOAuth2(
                getOAuth2Provider().getProvider(),
                oauth2UserInfo.getOpenId()
        );

        MemberDTO memberDTO;

        if (result.isSuccess() && result.getData() != null) {
            // 已绑定用户，直接返回
            memberDTO = result.getData();
            log.info("微信账号已绑定: openId={}, memberId={}",
                    oauth2UserInfo.getOpenId(), memberDTO.getMemberId());
        } else {
            // 未绑定用户，创建新用户或通过unionId关联
            memberDTO = handleUnboundOAuth2Account(oauth2UserInfo);
        }

        // 缓存OAuth2用户信息
        cacheOAuth2UserInfo(memberDTO.getMemberId(), oauth2UserInfo);

        return memberDTO;
    }
}

