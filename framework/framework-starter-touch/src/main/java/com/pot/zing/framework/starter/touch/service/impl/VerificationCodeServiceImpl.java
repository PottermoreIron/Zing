package com.pot.zing.framework.starter.touch.service.impl;

import com.pot.zing.framework.common.model.R;
import com.pot.zing.framework.common.util.RandomUtils;
import com.pot.zing.framework.starter.redis.service.RedisService;
import com.pot.zing.framework.starter.touch.enums.TouchChannelType;
import com.pot.zing.framework.starter.touch.exception.TouchException;
import com.pot.zing.framework.starter.touch.model.TouchRequest;
import com.pot.zing.framework.starter.touch.model.TouchResponse;
import com.pot.zing.framework.starter.touch.model.VerificationCodeRequest;
import com.pot.zing.framework.starter.touch.model.VerificationCodeResponse;
import com.pot.zing.framework.starter.touch.properties.TouchProperties;
import com.pot.zing.framework.starter.touch.properties.VerificationCodeProperties;
import com.pot.zing.framework.starter.touch.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.pot.zing.framework.starter.touch.properties.VerificationCodeProperties.*;

/**
 * @author: Pot
 * @created: 2025/10/19 16:56
 * @description: 验证码服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private final TouchServiceImpl touchService;
    private final RedisService redisService;
    private final TouchProperties touchProperties;
    private final VerificationCodeProperties verificationCodeProperties;

    @Override
    public R<VerificationCodeResponse> sendCode(VerificationCodeRequest request) {
        try {
            // 1. 参数校验
            validateRequest(request);

            // 2. 检查发送频率限制
            checkRateLimit(request.getTarget(), request.getBizType());

            // 3. 生成验证码
            String code = RandomUtils.generateRandomCode(request.getCodeLength());

            // 4. 构建触达请求
            TouchRequest touchRequest = buildTouchRequest(request, code);

            // 5. 发送验证码
            R<TouchResponse> sendResult = touchService.sendWithFallback(touchRequest);

            if (!sendResult.isSuccess()) {
                log.error("验证码发送失败: target={}, bizType={}",
                        request.getTarget(), request.getBizType());
                return R.fail("验证码发送失败");
            }

            // 6. 存储验证码到 Redis
            storeCode(request.getTarget(), code, request.getBizType(), request.getExpireSeconds());

            // 7. 返回结果
            TouchResponse touchResponse = sendResult.getData();
            VerificationCodeResponse response = VerificationCodeResponse.builder()
                    .messageId(touchResponse.getMessageId())
                    .code(isTestEnvironment() ? code : null)
                    .expireTime(LocalDateTime.now().plusSeconds(request.getExpireSeconds()))
                    .channelType(touchResponse.getChannelType())
                    .build();

            log.info("验证码发送成功: target={}, bizType={}, messageId={}",
                    request.getTarget(), request.getBizType(), touchResponse.getMessageId());

            return R.success(response);

        } catch (TouchException e) {
            log.error("验证码发送失败: target={}, bizType={}, error={}",
                    request.getTarget(), request.getBizType(), e.getMessage());
            return R.fail(e.getMessage());
        } catch (Exception e) {
            log.error("验证码发送异常: target={}, bizType={}",
                    request.getTarget(), request.getBizType(), e);
            return R.fail("验证码发送异常: " + e.getMessage());
        }
    }

    @Override
    public R<Boolean> validateCode(String target, String code, String bizType) {
        try {
            // 1. 获取存储的验证码
            String storedCode = getStoredCode(target, bizType);

            if (storedCode == null) {
                return R.fail("验证码不存在或已过期");
            }

            // 2. 校验验证码
            if (!storedCode.equals(code)) {
                recordValidateFailure(target, bizType);
                return R.fail("验证码错误");
            }

            // 3. 验证成功,删除验证码和失败记录
            deleteCode(target, bizType);
            deleteFailureRecord(target, bizType);

            log.info("验证码验证成功: target={}, bizType={}", target, bizType);
            return R.success(true);

        } catch (TouchException e) {
            log.error("验证码验证失败: target={}, bizType={}, error={}",
                    target, bizType, e.getMessage());
            return R.fail(e.getMessage());
        } catch (Exception e) {
            log.error("验证码验证异常: target={}, bizType={}", target, bizType, e);
            return R.fail("验证码验证异常: " + e.getMessage());
        }
    }

    @Override
    public void deleteCode(String target, String bizType) {
        String key = buildCodeKey(target, bizType);
        redisService.delete(key);
        log.debug("删除验证码: key={}", key);
    }

    /**
     * 参数校验
     */
    private void validateRequest(VerificationCodeRequest request) {
        if (request == null) {
            throw new TouchException("请求参数不能为空");
        }
        if (request.getTarget() == null || request.getTarget().trim().isEmpty()) {
            throw new TouchException("接收目标不能为空");
        }
        if (request.getBizType() == null || request.getBizType().trim().isEmpty()) {
            throw new TouchException("业务类型不能为空");
        }
        if (request.getCodeLength() <= 0) {
            throw new TouchException("验证码长度必须大于0");
        }
        if (request.getExpireSeconds() <= 0) {
            throw new TouchException("过期时间必须大于0");
        }
    }

    /**
     * 检查发送频率限制
     */
    private void checkRateLimit(String target, String bizType) {
        String rateLimitKey = buildRateLimitKey(target, bizType);

        // 使用 setIfAbsent 实现简单限流
        Boolean limited = redisService.setIfAbsent(
                rateLimitKey,
                "1",
                Duration.ofSeconds(verificationCodeProperties.getRateLimitSeconds())
        );

        if (!Boolean.TRUE.equals(limited)) {
            log.warn("验证码发送过于频繁: target={}, bizType={}", target, bizType);
            throw new TouchException("发送过于频繁,请" + verificationCodeProperties.getRateLimitSeconds() + "秒后再试");
        }
    }

    /**
     * 存储验证码
     */
    private void storeCode(String target, String code, String bizType, Long expireSeconds) {
        String key = buildCodeKey(target, bizType);
        redisService.set(key, code, Duration.ofSeconds(expireSeconds));
        log.debug("存储验证码: key={}, expireSeconds={}", key, expireSeconds);
    }

    /**
     * 获取存储的验证码
     */
    private String getStoredCode(String target, String bizType) {
        String key = buildCodeKey(target, bizType);
        return redisService.get(key, String.class);
    }

    /**
     * 记录验证失败次数
     */
    private void recordValidateFailure(String target, String bizType) {
        String failureKey = buildFailureKey(target, bizType);

        // 增加失败次数
        Long failures = redisService.increment(failureKey);

        if (failures != null) {
            // 第一次失败时设置过期时间
            if (failures == 1) {
                redisService.expire(failureKey, Duration.ofMinutes(verificationCodeProperties.getFailureExpireMinutes()));
            }

            // 失败次数过多,删除验证码
            if (failures >= verificationCodeProperties.getMaxFailureCount()) {
                deleteCode(target, bizType);
                log.warn("验证失败次数过多: target={}, bizType={}, failures={}",
                        target, bizType, failures);
                throw new TouchException("验证失败次数过多,请重新获取验证码");
            }
        }
    }

    /**
     * 删除失败记录
     */
    private void deleteFailureRecord(String target, String bizType) {
        String failureKey = buildFailureKey(target, bizType);
        redisService.delete(failureKey);
    }

    /**
     * 构建触达请求
     */
    private TouchRequest buildTouchRequest(VerificationCodeRequest request, String code) {
        Map<String, Object> params = new HashMap<>();
        params.put("code", code);
        params.put("expireMinutes", request.getExpireSeconds() / 60);

        if (request.getExtraParams() != null) {
            params.putAll(request.getExtraParams());
        }

        String templateId = getTemplateId(request.getChannelType(), request.getBizType());

        return TouchRequest.builder()
                .target(request.getTarget())
                .channelType(request.getChannelType())
                .templateId(templateId)
                .params(params)
                .bizType(request.getBizType())
                .build();
    }

    /**
     * 获取模板ID
     */
    private String getTemplateId(TouchChannelType channelType, String bizType) {
        return channelType.name().toLowerCase() + "_" + bizType + "_template";
    }

    /**
     * 是否测试环境
     */
    private boolean isTestEnvironment() {
        return touchProperties != null && "test".equals(touchProperties.getEnv());
    }

    /**
     * 构建验证码 Redis Key
     */
    private String buildCodeKey(String target, String bizType) {
        return redisService.buildKey(CODE_KEY_PREFIX, bizType, target);
    }

    /**
     * 构建限流 Redis Key
     */
    private String buildRateLimitKey(String target, String bizType) {
        return redisService.buildKey(RATE_LIMIT_KEY_PREFIX, bizType, target);
    }

    /**
     * 构建失败次数 Redis Key
     */
    private String buildFailureKey(String target, String bizType) {
        return redisService.buildKey(FAILURE_KEY_PREFIX, bizType, target);
    }
}
