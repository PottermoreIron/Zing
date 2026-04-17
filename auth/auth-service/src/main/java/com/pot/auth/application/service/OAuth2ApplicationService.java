package com.pot.auth.application.service;

import com.pot.auth.domain.oauth2.valueobject.OAuth2Provider;
import com.pot.auth.domain.port.OAuth2Port;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Application service for OAuth2 authorization flows.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "auth.oauth2.enabled", havingValue = "true")
public class OAuth2ApplicationService {

    private final OAuth2Port oauth2Port;

    /**
     * Builds the provider authorization URL for the OAuth2 authorization code flow.
     * If {@code state} is blank, a random UUID is generated to protect against
     * CSRF.
     */
    public String getAuthorizationUrl(String providerCode, String redirectUri, String state) {
        OAuth2Provider provider = OAuth2Provider.fromCode(providerCode);
        String resolvedState = (state != null && !state.isBlank()) ? state : UUID.randomUUID().toString();
        return oauth2Port.getAuthorizationUrl(provider, resolvedState, redirectUri);
    }
}
