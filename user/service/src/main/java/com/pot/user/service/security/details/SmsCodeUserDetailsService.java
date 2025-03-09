package com.pot.user.service.security.details;

import com.pot.user.service.entity.User;
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
public class SmsCodeUserDetailsService implements UserDetailsService {
    private final UserService userService;

    @Autowired
    public SmsCodeUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    // find user by phone
    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        log.info("loadUserByUsername phone={}", phone);
        User user = userService.lambdaQuery().eq(User::getPhone, phone).one();
        log.info("loadUserByUsername user={}", user);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return LoginUser.builder()
                .user(user)
                .build();
    }
}
