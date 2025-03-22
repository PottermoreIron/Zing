package com.pot.user.service.controller;

import com.pot.common.R;
import com.pot.user.service.controller.request.RegisterRequest;
import com.pot.user.service.controller.request.SendCodeRequest;
import com.pot.user.service.controller.request.SendSmsCodeRequest;
import com.pot.user.service.service.SmsCodeService;
import com.pot.user.service.strategy.RegisterStrategy;
import com.pot.user.service.strategy.factory.RegisterStrategyFactory;
import com.sankuai.inf.leaf.service.SegmentService;
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
    private final SmsCodeService smsCodeService;
    private final SegmentService segmentService;

    @RequestMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterRequest request) {
        log.info("request={}", request);
        RegisterStrategy strategy = strategyFactory.getStrategyByCode(request.getType());
        strategy.register(request);
        return R.success("注册成功");
    }

    @RequestMapping("/send/sms/code")
    public R<Void> sendSms(@Valid @RequestBody SendCodeRequest request) {
        String phone = ((SendSmsCodeRequest) request).getPhone();
        smsCodeService.sendSmsCode(phone);
        return R.success("发送成功");
    }

    @RequestMapping("/test")
    public R<Long> test() {
        Long id = segmentService.getId("user").getId();
        return R.success(id, "test");
    }

}
