package com.pot.user.service.controller;

import com.pot.user.service.controller.request.RegisterRequest;
import com.pot.user.service.strategy.RegisterStrategy;
import com.pot.user.service.strategy.factory.RegisterStrategyFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author pot
 * @since 2025-02-25
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final RegisterStrategyFactory strategyFactory;

    @RequestMapping("/register")
    public String test(@Valid @RequestBody RegisterRequest request) {
        log.info("request={}", request);
        RegisterStrategy strategy = strategyFactory.getStrategyByCode(request.getType());
        strategy.register(request);
        return "success";
    }

}
