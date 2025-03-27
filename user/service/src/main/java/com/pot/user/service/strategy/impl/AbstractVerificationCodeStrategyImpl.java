package com.pot.user.service.strategy.impl;

import com.pot.common.enums.ResultCode;
import com.pot.user.service.enums.VerificationCodeType;
import com.pot.user.service.exception.BusinessException;
import com.pot.user.service.strategy.VerificationCodeStrategy;
import com.pot.user.service.utils.RedisUtils;

import static com.pot.user.service.utils.RandomStringGenerator.generateRandomCode;

/**
 * @author: Pot
 * @created: 2025/3/27 23:47
 * @description: 抽象实现验证码服务类
 */
public abstract class AbstractVerificationCodeStrategyImpl implements VerificationCodeStrategy {
    private static final String CODE_KEY_PREFIX = "verification_code:";
    protected static final int CODE_LENGTH = 6;
    protected static final long CODE_EXPIRE = 300;

    @Override
    public void sendCode(String target) {
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
    public VerificationCodeType getVerificationCodeType() {
        return null;
    }

    private String buildRedisKey(String target) {
        return getVerificationCodeType().name().toLowerCase() + ":" + CODE_KEY_PREFIX + target;
    }

    private void storeCode(String target, String code) {
        RedisUtils.set(buildRedisKey(target), code, CODE_EXPIRE);
    }

    private String getStoredCode(String target) {
        return RedisUtils.get(buildRedisKey(target));
    }

    private void deleteCode(String target) {
        RedisUtils.delete(buildRedisKey(target));
    }

    protected abstract void doSend(String target, String code);

    protected abstract void checkTarget(String target);
}
