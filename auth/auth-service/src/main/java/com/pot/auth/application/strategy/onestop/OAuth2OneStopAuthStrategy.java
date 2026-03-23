package com.pot.auth.application.strategy.onestop;

import com.pot.auth.application.strategy.AbstractOneStopAuthStrategyImpl;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.domain.context.OneStopAuthContext;
import com.pot.auth.domain.oauth2.entity.OAuth2UserInfo;
import com.pot.auth.domain.oauth2.valueobject.OAuth2AuthorizationCode;
import com.pot.auth.domain.oauth2.valueobject.OAuth2Provider;
import com.pot.auth.domain.port.OAuth2Port;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import com.pot.auth.domain.shared.valueobject.Email;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.domain.validation.ValidationChain;
import com.pot.auth.interfaces.dto.onestop.OAuth2AuthRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "auth.oauth2.enabled", havingValue = "true")
public class OAuth2OneStopAuthStrategy
        extends AbstractOneStopAuthStrategyImpl<OAuth2AuthRequest> {

    private final OAuth2Port oauth2Port;
    private final UserModulePortFactory userModulePortFactory;
    private static final ThreadLocal<OAuth2UserInfo> USER_INFO_CACHE = new ThreadLocal<>();

    public OAuth2OneStopAuthStrategy(
            JwtTokenService jwtTokenService,
            OAuth2Port oauth2Port,
            UserModulePortFactory userModulePortFactory,
            UserDefaultsGenerator userDefaultsGenerator) {
        super(jwtTokenService, createValidationChain(), userDefaultsGenerator);
        this.oauth2Port = oauth2Port;
        this.userModulePortFactory = userModulePortFactory;
    }

    private static ValidationChain<OneStopAuthContext> createValidationChain() {
        return new ValidationChain<>();
    }

    @Override
    protected UserDTO findUser(OneStopAuthContext context) {
        OAuth2AuthRequest request = (OAuth2AuthRequest) context.request();
        try {
            OAuth2UserInfo oauth2UserInfo = getOrFetchOAuth2UserInfo(request);
            UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
            return userModulePort.findUserByOAuth2(
                    request.provider().getCode(),
                    oauth2UserInfo.openId().value()).orElse(null);
        } catch (Exception e) {
            throw new DomainException("获取OAuth2用户信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    protected void validateCredentialForLogin(OneStopAuthContext context, UserDTO user) {
        OAuth2AuthRequest request = (OAuth2AuthRequest) context.request();
        log.debug("[OAuth2认证] 用户已绑定，直接登录: userId={}, provider={}", user.userId(), request.provider());
    }

    @Override
    protected void validateCredentialForRegister(OneStopAuthContext context) {
        getOrFetchOAuth2UserInfo((OAuth2AuthRequest) context.request());
    }

    @Override
    protected UserDTO createUserWithDefaults(OneStopAuthContext context) {
        OAuth2AuthRequest request = (OAuth2AuthRequest) context.request();
        OAuth2UserInfo oauth2UserInfo = getOrFetchOAuth2UserInfo(request);
        String username = userDefaultsGenerator.generateUsernameFromEmail(oauth2UserInfo.email());
        String password = userDefaultsGenerator.generateRandomPassword();
        String avatarUrl = oauth2UserInfo.avatarUrl() != null
                ? oauth2UserInfo.avatarUrl()
                : userDefaultsGenerator.getDefaultAvatarUrl();

        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        CreateUserCommand command = CreateUserCommand.builder()
                .username(username)
                .email(Email.of(oauth2UserInfo.email()))
                .password(Password.of(password))
                .nickname(oauth2UserInfo.nickname())
                .avatarUrl(avatarUrl)
                .emailVerified(oauth2UserInfo.emailVerified() != null && oauth2UserInfo.emailVerified())
                .oauth2Provider(request.provider().getCode())
                .oauth2OpenId(oauth2UserInfo.openId().value())
                .build();

        var userId = userModulePort.createUser(command);
        return userModulePort.findById(userId)
                .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
    }

    @Override
    public boolean supports(AuthType authType) {
        return authType == AuthType.OAUTH2;
    }

    @Override
    public AuthType getSupportedAuthType() {
        return AuthType.OAUTH2;
    }

    @Override
    protected void cleanupAfterAuthentication() {
        USER_INFO_CACHE.remove();
    }

    private OAuth2UserInfo getOrFetchOAuth2UserInfo(OAuth2AuthRequest request) {
        OAuth2UserInfo cached = USER_INFO_CACHE.get();
        if (cached != null) {
            return cached;
        }
        OAuth2Provider provider = OAuth2Provider.fromCode(request.provider().getCode());
        OAuth2AuthorizationCode code = OAuth2AuthorizationCode.of(request.code());
        OAuth2UserInfo oauth2UserInfo = oauth2Port.getUserInfo(provider, code, request.state());
        USER_INFO_CACHE.set(oauth2UserInfo);
        return oauth2UserInfo;
    }
}