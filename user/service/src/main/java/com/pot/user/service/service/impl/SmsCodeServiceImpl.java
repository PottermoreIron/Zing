package com.pot.user.service.service.impl;

import com.pot.common.R;
import com.pot.common.enums.ResultCode;
import com.pot.common.utils.ValidationUtils;
import com.pot.user.service.exception.BusinessException;
import com.pot.user.service.service.SmsCodeService;
import com.pot.user.service.utils.RedisUtils;
import org.springframework.stereotype.Service;

import static com.pot.user.service.utils.RandomStringGenerator.generateRandomCode;

/**
 * @author: Pot
 * @created: 2025/3/16 22:56
 * @description: 验证码服务接口实现类
 */
@Service
public class SmsCodeServiceImpl implements SmsCodeService {
    private static final String SMS_CODE_KEY_PREFIX = "sms_code:";

    @Override
    public void sendSmsCode(String phone) {
        checkPhone(phone);
        String smsCode = generateRandomCode(6);
        // todo 发送验证码
        // 保存验证码到redis
        String key = SMS_CODE_KEY_PREFIX + phone;
        RedisUtils.set(key, smsCode);
    }

    @Override
    public void validateSmsCode(String phone, String code) {
        checkPhone(phone);
        String key = SMS_CODE_KEY_PREFIX + phone;
        String smsCode = RedisUtils.get(key);
        if (smsCode == null) {
            throw new BusinessException(ResultCode.VERIFICATION_CODE_NOT_EXIST);
        }
        if (!smsCode.equals(code)) {
            throw new BusinessException(ResultCode.VERIFICATION_CODE_ERROR);
        }
        RedisUtils.delete(key);
        R.success("验证码正确");
    }

    private void checkPhone(String phone) {
        if (!ValidationUtils.isPhone(phone)) {
            throw new BusinessException(ResultCode.PHONE_NOT_LEGAL);
        }
    }
}
