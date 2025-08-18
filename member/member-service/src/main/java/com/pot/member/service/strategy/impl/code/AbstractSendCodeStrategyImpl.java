package com.pot.member.service.strategy.impl.code;

import com.pot.common.enums.ResultCode;
import com.pot.common.exception.BusinessException;
import com.pot.common.redis.RedisService;
import com.pot.member.service.controller.request.SendCodeRequest;
import com.pot.member.service.enums.SendCodeChannelEnum;
import com.pot.member.service.strategy.SendCodeStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.pot.common.utils.RandomUtils.generateRandomCode;

/**
 * @author: Pot
 * @created: 2025/3/27 23:47
 * @description: 抽象实现验证码服务类
 */
@Service
@RequiredArgsConstructor
public abstract class AbstractSendCodeStrategyImpl implements SendCodeStrategy {

    protected final RedisService redisService;

    private static final String CODE_KEY_PREFIX = "verification_code:";
    protected static final int CODE_LENGTH = 6;
    protected static final long CODE_EXPIRE = 300;
    protected final Duration CODE_EXPIRE_TIME = Duration.ofSeconds(CODE_EXPIRE);
    protected String target;

    @Override
    public void sendCode(SendCodeRequest request) {
        setTarget(request);
        checkTarget(target);
        String code = generateRandomCode(CODE_LENGTH);
        doSend(target, code);
        storeCode(target, code);
    }

    @Override
    public void validateCode(String target, String code) {
        checkTarget(target);
        String storedCode = getStoredCode(target);
        if (storedCode == null) {
            throw new BusinessException(ResultCode.VERIFICATION_CODE_NOT_EXIST);
        }
        if (!storedCode.equals(code)) {
            throw new BusinessException(ResultCode.VERIFICATION_CODE_ERROR);
        }
        deleteCode(target);
    }

    @Override
    public SendCodeChannelEnum getVerificationCodeType() {
        return null;
    }

    private String buildRedisKey(String target) {
        return getVerificationCodeType().name().toLowerCase() + ":" + CODE_KEY_PREFIX + target;
    }

    private void storeCode(String target, String code) {
        redisService.set(buildRedisKey(target), code, CODE_EXPIRE_TIME);
    }

    private String getStoredCode(String target) {
        return redisService.get(buildRedisKey(target)).toString();
    }

    private void deleteCode(String target) {
        redisService.delete(buildRedisKey(target));
    }

    protected abstract void doSend(String target, String code);

    protected abstract void checkTarget(String target);

    protected abstract void setTarget(SendCodeRequest request);
}
