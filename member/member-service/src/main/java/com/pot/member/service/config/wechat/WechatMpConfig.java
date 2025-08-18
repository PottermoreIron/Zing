package com.pot.member.service.config.wechat;

import com.pot.common.utils.CollectionUtils;
import com.pot.member.service.handler.wechat.AbstractWechatMpHandler;
import com.pot.member.service.handler.wechat.DefaultWechatMpHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Pot
 * @created: 2025/4/12 21:46
 * @description: wechat config
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(WechatMpProperties.class)
@Slf4j
public class WechatMpConfig {

    private final WechatMpProperties wechatMpProperties;
    private final List<AbstractWechatMpHandler> handlers;

    @Bean
    public WxMpService wxMpService() {
        List<WechatMpProperties.MpConfig> configs = wechatMpProperties.getConfigs();
        if (CollectionUtils.isEmpty(configs)) {
            return null;
        }
        WxMpService wxMpService = new WxMpServiceImpl();
        wxMpService.setMultiConfigStorages(configs.stream()
                .map(this::buildConfigStorage)
                .collect(Collectors.toMap(WxMpDefaultConfigImpl::getAppId, config -> config, (o, n) -> o)));
        return wxMpService;
    }

    @Bean
    public WxMpMessageRouter messageRouter(WxMpService wxMpService) {
        WxMpMessageRouter router = new WxMpMessageRouter(wxMpService);
        for (AbstractWechatMpHandler handler : handlers) {
            log.info("Registering handler: {} for msgType: {} and eventType: {}",
                    handler.getClass().getSimpleName(), handler.supportedMsgType(), handler.supportedEventType());
        }
        handlers.forEach(handler ->
                router.rule()
                        .async(false)
                        .msgType(handler.supportedMsgType())
                        .event(handler.supportedEventType())
                        .handler(handler)
                        .end()
        );
        // 默认消息处理
        router.rule().async(false).handler(getDefaultHandler()).end();
        return router;
    }

    private WxMpDefaultConfigImpl buildConfigStorage(WechatMpProperties.MpConfig config) {
        WxMpDefaultConfigImpl configStorage = new WxMpDefaultConfigImpl();
        configStorage.setAppId(config.getAppId());
        configStorage.setSecret(config.getSecret());
        configStorage.setToken(config.getToken());
        configStorage.setAesKey(config.getAesKey());
        return configStorage;
    }

    private WxMpMessageHandler getDefaultHandler() {
        return handlers.stream()
                .filter(h -> h instanceof DefaultWechatMpHandler)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing default handler"));
    }
}
