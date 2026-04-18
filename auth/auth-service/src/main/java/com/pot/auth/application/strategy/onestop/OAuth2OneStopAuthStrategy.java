package com.pot.auth.application.strategy.onestop;

import com.pot.auth.application.strategy.AbstractOneStopAuthStrategyImpl;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.application.context.OneStopAuthContext;
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
import com.pot.auth.application.command.OneStopAuthCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(name = "auth.oauth2.enabled", havingValue = "true")
public class OAuth2OneStopAuthStrategy
        extends AbstractOneStopAuthStrategyImpl {

    private final OAuth2Port oauth2Port;
    private final UserModulePortFactory userModulePortFactory;
    private static final ThreadLocal<OAuth2UserInfo> USER_INFO_CACHE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> NEEDS_BINDING = new ThreadLocal<>();

    public OAuth2OneStopAuthStrategy(
            JwtTokenService jwtTokenService,
            OAuth2Port oauth2Port,
            UserModulePortFactory userModulePortFactory,
            UserDefaultsGenerator userDefaultsGenerator) {
        super(jwtTokenService, userDefaultsGenerator);
        this.oauth2Port = oauth2Port;
        this.userModulePortFactory = userModulePortFactory;
    }

    @Override
    protected UserDTO findUser(OneStopAuthContext context) {
        var request = context.request();
        try {
            OAuth2UserInfo oauth2UserInfo = getOrFetchOAuth2UserInfo(request);
            UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());

            Optional<UserDTO> byOAuth2 = userModulePort.findUserByOAuth2(
                    request.oauth2ProviderCode(), oauth2UserInfo.openId().value());
            if (byOAuth2.isPresent()) {
                return byOAuth2.get();
            }

            // Account linking: only link when the provider has verified the email,
            // to prevent account takeover via unverified email claims.
            if (StringUtils.hasText(oauth2UserInfo.email()) && Boolean.TRUE.equals(oauth2UserInfo.emailVerified())) {
                Optional<UserDTO> byEmail = userModulePort.findByEmail(oauth2UserInfo.email());
                if (byEmail.isPresent()) {
                    NEEDS_BINDING.set(Boolean.TRUE);
                    return byEmail.get();
                }
            }

            return null;
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException(AuthResultCode.OAUTH2_CODE_INVALID, e);
        }
    }

    @Override
    protected void validateCredentialForLogin(OneStopAuthContext context, UserDTO user) {
        var request = context.request();
        log.debug("[OAuth2Auth] User already bound, executing login — userId={}, provider={}", user.userId(),
                request.oauth2ProviderCode());
    }

    @Override
    protected void beforeLogin(UserDTO user, OneStopAuthContext context) {
        if (!Boolean.TRUE.equals(NEEDS_BINDING.get())) {
            return;
        }
        var request = context.request();
        OAuth2UserInfo oauth2UserInfo = getOrFetchOAuth2UserInfo(request);
        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        Map<String, Object> tokenInfo = new HashMap<>();
        if (oauth2UserInfo.accessToken() != null) {
            tokenInfo.put("accessToken", oauth2UserInfo.accessToken());
        }
        if (oauth2UserInfo.refreshToken() != null) {
            tokenInfo.put("refreshToken", oauth2UserInfo.refreshToken());
        }
        userModulePort.bindOAuth2(user.userId(), request.oauth2ProviderCode(),
                oauth2UserInfo.openId().value(), tokenInfo);
        log.info("[OAuth2Auth] Linked OAuth2 to existing account — userId={}, provider={}",
                user.userId(), request.oauth2ProviderCode());
    }

    @Override
    protected void validateCredentialForRegister(OneStopAuthContext context) {
        getOrFetchOAuth2UserInfo(context.request());
    }

    @Override
    protected UserDTO createUserWithDefaults(OneStopAuthContext context) {
        var request = context.request();
        OAuth2UserInfo oauth2UserInfo = getOrFetchOAuth2UserInfo(request);
        String password = userDefaultsGenerator.generateRandomPassword();
        String avatarUrl = oauth2UserInfo.avatarUrl() != null
                ? oauth2UserInfo.avatarUrl()
                : userDefaultsGenerator.getDefaultAvatarUrl();

        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        String generatedNickname = StringUtils.hasText(oauth2UserInfo.email())
                ? generateAvailableNickname(userModulePort,
                        () -> userDefaultsGenerator.generateNicknameFromEmail(oauth2UserInfo.email()))
                : generateAvailableNickname(userModulePort, userDefaultsGenerator::generateNickname);
        CreateUserCommand command = CreateUserCommand.builder()
                .nickname(generatedNickname)
                .email(StringUtils.hasText(oauth2UserInfo.email()) ? Email.of(oauth2UserInfo.email()) : null)
                .password(Password.of(password))
                .displayName(oauth2UserInfo.nickname())
                .avatarUrl(avatarUrl)
                .emailVerified(oauth2UserInfo.emailVerified() != null && oauth2UserInfo.emailVerified())
                .oauth2Provider(request.oauth2ProviderCode())
                .oauth2OpenId(oauth2UserInfo.openId().value())
                .oauth2AccessToken(oauth2UserInfo.accessToken())
                .oauth2RefreshToken(oauth2UserInfo.refreshToken())
                .oauth2TokenExpiresAt(oauth2UserInfo.expiresIn())
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
        NEEDS_BINDING.remove();
    }

    private OAuth2UserInfo getOrFetchOAuth2UserInfo(OneStopAuthCommand request) {
        OAuth2UserInfo cached = USER_INFO_CACHE.get();
        if (cached != null) {
            return cached;
        }
        OAuth2Provider provider = OAuth2Provider.fromCode(request.oauth2ProviderCode());
        OAuth2AuthorizationCode code = OAuth2AuthorizationCode.of(request.code());
        OAuth2UserInfo oauth2UserInfo = oauth2Port.getUserInfo(provider, code, request.redirectUri());
        USER_INFO_CACHE.set(oauth2UserInfo);
        return oauth2UserInfo;
    }
}