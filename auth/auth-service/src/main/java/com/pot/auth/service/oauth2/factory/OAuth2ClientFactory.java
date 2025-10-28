package com.pot.auth.service.oauth2.factory;

import com.pot.auth.service.enums.OAuth2Provider;
import com.pot.auth.service.oauth2.OAuth2ClientService;
import com.pot.zing.framework.common.excption.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: Pot
 * @created: 2025/10/22
 * @description: OAuth2客户端工厂 - 根据提供商选择对应的OAuth2客户端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2ClientFactory {

    private final List<OAuth2ClientService> oauth2ClientServices;

    private volatile Map<OAuth2Provider, OAuth2ClientService> clientServiceMap;

    /**
     * 获取OAuth2客户端服务
     *
     * @param provider OAuth2提供商
     * @return OAuth2ClientService
     */
    public OAuth2ClientService getClientService(OAuth2Provider provider) {
        if (clientServiceMap == null) {
            synchronized (this) {
                if (clientServiceMap == null) {
                    clientServiceMap = oauth2ClientServices.stream()
                            .collect(Collectors.toMap(
                                    OAuth2ClientService::getProvider,
                                    Function.identity(),
                                    (existing, replacement) -> {
                                        log.warn("发现重复的OAuth2客户端: {}, 使用第一个",
                                                existing.getProvider());
                                        return existing;
                                    }
                            ));
                    log.info("OAuth2客户端工厂初始化完成，共加载 {} 个客户端: {}",
                            clientServiceMap.size(),
                            clientServiceMap.keySet());
                }
            }
        }

        OAuth2ClientService clientService = clientServiceMap.get(provider);
        if (clientService == null) {
            throw new BusinessException("不支持的OAuth2提供商: " + provider.getDisplayName());
        }
        return clientService;
    }

    /**
     * 检查是否支持该OAuth2提供商
     *
     * @param provider OAuth2提供商
     * @return 是否支持
     */
    public boolean supports(OAuth2Provider provider) {
        if (clientServiceMap == null) {
            getClientService(provider); // 触发初始化
        }
        return clientServiceMap.containsKey(provider);
    }
}

