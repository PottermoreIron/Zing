package com.pot.auth.application.strategy.onestop;

import com.pot.auth.application.strategy.AbstractOneStopAuthStrategyImpl;
import com.pot.auth.domain.authentication.service.JwtTokenService;
import com.pot.auth.application.context.OneStopAuthContext;
import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.UserModulePortFactory;
import com.pot.auth.domain.port.WeChatPort;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.enums.AuthResultCode;
import com.pot.auth.domain.shared.enums.AuthType;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.application.command.OneStopAuthCommand;
import com.pot.auth.domain.wechat.entity.WeChatUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "auth.wechat.enabled", havingValue = "true")
public class WeChatOneStopAuthStrategy
        extends AbstractOneStopAuthStrategyImpl {

    private final WeChatPort weChatPort;
    private final UserModulePortFactory userModulePortFactory;
    private static final ThreadLocal<WeChatUserInfo> USER_INFO_CACHE = new ThreadLocal<>();

    public WeChatOneStopAuthStrategy(
            JwtTokenService jwtTokenService,
            WeChatPort weChatPort,
            UserModulePortFactory userModulePortFactory,
            UserDefaultsGenerator userDefaultsGenerator) {
        super(jwtTokenService, userDefaultsGenerator);
        this.weChatPort = weChatPort;
        this.userModulePortFactory = userModulePortFactory;
    }

    @Override
    protected UserDTO findUser(OneStopAuthContext context) {
        var request = context.request();
        try {
            WeChatUserInfo weChatUserInfo = getOrFetchWeChatUserInfo(request);
            UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
            return userModulePort.findUserByWeChat(weChatUserInfo.getOpenId()).orElse(null);
        } catch (Exception e) {
            throw new DomainException(AuthResultCode.WECHAT_CODE_INVALID, e);
        }
    }

    @Override
    protected void validateCredentialForLogin(OneStopAuthContext context, UserDTO user) {
        log.debug("[WeChatAuth] User already bound, executing login — userId={}", user.userId());
    }

    @Override
    protected void validateCredentialForRegister(OneStopAuthContext context) {
        getOrFetchWeChatUserInfo(context.request());
    }

    @Override
    protected UserDTO createUserWithDefaults(OneStopAuthContext context) {
        var request = context.request();
        WeChatUserInfo weChatUserInfo = getOrFetchWeChatUserInfo(request);
        String password = userDefaultsGenerator.generateRandomPassword();
        String avatarUrl = weChatUserInfo.getAvatar() != null
                ? weChatUserInfo.getAvatar()
                : userDefaultsGenerator.getDefaultAvatarUrl();

        UserModulePort userModulePort = userModulePortFactory.getPort(request.userDomain());
        String generatedNickname = generateAvailableNickname(userModulePort, userDefaultsGenerator::generateNickname);
        CreateUserCommand command = CreateUserCommand.builder()
                .nickname(generatedNickname)
                .password(Password.of(password))
                .displayName(weChatUserInfo.getDisplayName())
                .avatarUrl(avatarUrl)
                .weChatOpenId(weChatUserInfo.getOpenId())
                .weChatUnionId(weChatUserInfo.getUnionId())
                .build();

        var userId = userModulePort.createUser(command);
        return userModulePort.findById(userId)
                .orElseThrow(() -> new DomainException(AuthResultCode.USER_NOT_FOUND));
    }

    @Override
    public boolean supports(AuthType authType) {
        return authType == AuthType.WECHAT;
    }

    @Override
    public AuthType getSupportedAuthType() {
        return AuthType.WECHAT;
    }

    @Override
    protected void cleanupAfterAuthentication() {
        USER_INFO_CACHE.remove();
    }

    private WeChatUserInfo getOrFetchWeChatUserInfo(OneStopAuthCommand request) {
        WeChatUserInfo cached = USER_INFO_CACHE.get();
        if (cached != null) {
            return cached;
        }
        WeChatUserInfo weChatUserInfo = weChatPort.getUserInfo(request.code(), request.state());
        USER_INFO_CACHE.set(weChatUserInfo);
        return weChatUserInfo;
    }
}