package com.pot.auth.service.oauth2.wechat;

import com.pot.auth.service.config.OAuth2ClientProperties;
import com.pot.auth.service.dto.wechat.WeChatQrCodeResponse;
import com.pot.auth.service.dto.wechat.WeChatScanStatusResponse;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.starter.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.UUID;

/**
 * @author: Pot
 * @created: 2025/10/23
 * @description: 微信扫码登录服务 - 核心业务逻辑
 * <p>
 * 主要功能：
 * 1. 生成微信扫码登录二维码
 * 2. 管理扫码状态
 * 3. 提供轮询接口供前端查询扫码状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeChatQrCodeService {

    // Redis缓存key前缀
    private static final String SCAN_STATUS_PREFIX = "wechat:scan:status:";
    private static final String SCAN_CODE_PREFIX = "wechat:scan:code:";
    // 二维码有效期：5分钟
    private static final Duration QR_EXPIRE_DURATION = Duration.ofMinutes(5);
    private final OAuth2ClientProperties oauth2Properties;
    private final RedisService redisService;

    /**
     * 生成微信扫码登录二维码URL
     *
     * @param state 防CSRF的state参数
     * @return 二维码信息
     */
    public WeChatQrCodeResponse generateQrCode(String state) {
        OAuth2ClientProperties.OAuth2ClientConfig config = getWeChatConfig();

        log.info("生成微信扫码登录二维码: state={}", state);

        // 构建微信授权URL
        // 文档: https://developers.weixin.qq.com/doc/oplatform/Website_App/WeChat_Login/Wechat_Login.html
        String authUrl = UriComponentsBuilder
                .fromHttpUrl(config.getAuthorizationUri())
                .queryParam("appid", config.getClientId())
                .queryParam("redirect_uri", config.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", config.getScope())
                .queryParam("state", state)
                .fragment("wechat_redirect")
                .build()
                .toUriString();

        // 初始化扫码状态为PENDING
        String statusKey = SCAN_STATUS_PREFIX + state;
        redisService.set(statusKey, WeChatScanStatusResponse.ScanStatus.PENDING.name(), QR_EXPIRE_DURATION);

        // 生成唯一二维码ID
        String qrCodeId = UUID.randomUUID().toString();

        log.info("微信扫码二维码生成成功: state={}, qrCodeId={}", state, qrCodeId);

        return WeChatQrCodeResponse.builder()
                .qrCodeUrl(authUrl)
                .state(state)
                .expireSeconds((int) QR_EXPIRE_DURATION.getSeconds())
                .qrCodeId(qrCodeId)
                .build();
    }

    /**
     * 轮询扫码状态
     * 前端通过此接口轮询获取扫码状态
     *
     * @param state state参数
     * @return 扫码状态信息
     */
    public WeChatScanStatusResponse pollScanStatus(String state) {
        String statusKey = SCAN_STATUS_PREFIX + state;
        String status = redisService.get(statusKey, String.class);

        if (status == null) {
            log.debug("二维码已过期: state={}", state);
            return WeChatScanStatusResponse.expired();
        }

        log.debug("查询扫码状态: state={}, status={}", state, status);

        // 根据不同状态返回响应
        WeChatScanStatusResponse.ScanStatus scanStatus;
        try {
            scanStatus = WeChatScanStatusResponse.ScanStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            log.warn("未知的扫码状态: state={}, status={}", state, status);
            return WeChatScanStatusResponse.expired();
        }

        switch (scanStatus) {
            case PENDING:
                return WeChatScanStatusResponse.pending();
            case SCANNED:
                return WeChatScanStatusResponse.scanned();
            case CONFIRMED:
                // 获取授权码
                String codeKey = SCAN_CODE_PREFIX + state;
                String code = redisService.get(codeKey, String.class);
                return WeChatScanStatusResponse.confirmed(code);
            case CANCELLED:
                return WeChatScanStatusResponse.builder()
                        .status(WeChatScanStatusResponse.ScanStatus.CANCELLED.name())
                        .message("用户取消授权")
                        .build();
            case EXPIRED:
            default:
                return WeChatScanStatusResponse.expired();
        }
    }

    /**
     * 更新扫码状态（由回调接口调用）
     *
     * @param state  状态参数
     * @param status 新状态
     */
    public void updateScanStatus(String state, WeChatScanStatusResponse.ScanStatus status) {
        String statusKey = SCAN_STATUS_PREFIX + state;
        redisService.set(statusKey, status.name(), QR_EXPIRE_DURATION);
        log.info("更新扫码状态: state={}, status={}", state, status);
    }

    /**
     * 更新扫码状态并保存授权码
     *
     * @param state 状态参数
     * @param code  授权码
     */
    public void updateScanStatusWithCode(String state, String code) {
        // 更新状态为已确认
        String statusKey = SCAN_STATUS_PREFIX + state;
        redisService.set(statusKey, WeChatScanStatusResponse.ScanStatus.CONFIRMED.name(), QR_EXPIRE_DURATION);

        // 保存授权码
        String codeKey = SCAN_CODE_PREFIX + state;
        redisService.set(codeKey, code, QR_EXPIRE_DURATION);

        log.info("更新扫码状态为已确认: state={}, code={}", state, code);
    }

    /**
     * 获取微信OAuth2配置
     */
    private OAuth2ClientProperties.OAuth2ClientConfig getWeChatConfig() {
        OAuth2ClientProperties.OAuth2ClientConfig config = oauth2Properties.getClients().get("wechat");

        if (config == null || !config.getEnabled()) {
            throw new BusinessException("微信OAuth2登录未配置或未启用");
        }

        return config;
    }
}

