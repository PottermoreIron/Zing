package com.pot.auth.application.service;

import com.pot.auth.domain.authentication.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutApplicationService {

    private final JwtTokenService jwtTokenService;

        public void logout(String accessToken, String refreshToken) {
        log.info("[登出] 执行登出操作");
        jwtTokenService.logout(accessToken, refreshToken);
        log.info("[登出] 登出操作完成");
    }
}
