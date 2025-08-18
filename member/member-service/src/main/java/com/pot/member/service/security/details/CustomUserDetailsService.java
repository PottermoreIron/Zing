package com.pot.member.service.security.details;

import com.pot.member.service.entity.User;
import com.pot.member.service.enums.LoginRegisterEnum;
import com.pot.member.service.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * @author: Pot
 * @created: 2025/2/25 23:19
 * @description: 测试spring security
 */
@Slf4j
@Component
public class CustomUserDetailsService implements UserDetailsService {
    private final UserService userService;

    @Autowired
    public CustomUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Deprecated
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        throw new UnsupportedOperationException("Use loadUserByIdentifier instead");
    }

    public UserDetails loadUserByIdentifier(String identifier, LoginRegisterEnum loginType) {
        log.info("Loading user by {}: {}", loginType, identifier);
        User user;
        switch (loginType) {
            case USERNAME_PASSWORD -> user = userService.lambdaQuery()
                    .eq(User::getNickname, identifier)
                    .one();
            case PHONE_PASSWORD, PHONE_CODE -> user = userService.lambdaQuery()
                    .eq(User::getPhone, identifier)
                    .one();
            case EMAIL_PASSWORD, EMAIL_CODE -> user = userService.lambdaQuery()
                    .eq(User::getEmail, identifier)
                    .one();
            default -> throw new IllegalArgumentException("Unsupported login type: " + loginType);
        }
        if (user == null) {
            return null;
        }
        return LoginUser.builder()
                .user(user)
                .build();
    }
}
