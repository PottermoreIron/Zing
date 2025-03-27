package com.pot.user.service.security.details;

import com.pot.common.enums.ResultCode;
import com.pot.user.service.entity.User;
import com.pot.user.service.exception.BusinessException;
import com.pot.user.service.service.UserService;
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

    // find user by phone
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        log.info("loadUserByUsername identifier={}", identifier);
        // 这个地方需要保证Nickname不能全是数字, 避免和手机号冲突
        User user = userService.lambdaQuery()
                .or().eq(User::getPhone, identifier)
                .or().eq(User::getNickname, identifier)
                .or().eq(User::getEmail, identifier)
                .one();
        log.info("loadUserByUsername user={}", user);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        return LoginUser.builder()
                .user(user)
                .build();
    }
}
