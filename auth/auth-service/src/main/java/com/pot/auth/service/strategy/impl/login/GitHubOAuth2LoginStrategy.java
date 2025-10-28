package com.pot.auth.service.strategy.impl.login;

import com.pot.auth.service.enums.LoginType;
import com.pot.auth.service.enums.OAuth2Provider;
import com.pot.auth.service.oauth2.factory.OAuth2ClientFactory;
import com.pot.auth.service.service.adapter.VerificationCodeAdapter;
import com.pot.auth.service.utils.UserTokenUtils;
import com.pot.member.facade.api.MemberFacade;
import com.pot.zing.framework.starter.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/10/22
 * @description: GitHub OAuth2登录策略
 */
@Slf4j
@Component
public class GitHubOAuth2LoginStrategy extends AbstractOAuth2LoginStrategy {

    public GitHubOAuth2LoginStrategy(
            MemberFacade memberFacade,
            UserTokenUtils userTokenUtils,
            VerificationCodeAdapter verificationCodeAdapter,
            OAuth2ClientFactory oauth2ClientFactory,
            RedisService redisService) {
        super(memberFacade, userTokenUtils, verificationCodeAdapter, oauth2ClientFactory, redisService);
    }

    @Override
    public LoginType getLoginType() {
        return LoginType.OAUTH2_GITHUB;
    }

    @Override
    protected OAuth2Provider getOAuth2Provider() {
        return OAuth2Provider.GITHUB;
    }
}

