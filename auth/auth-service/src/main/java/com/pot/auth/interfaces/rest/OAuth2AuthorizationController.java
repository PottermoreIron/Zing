package com.pot.auth.interfaces.rest;

import com.pot.auth.application.service.OAuth2ApplicationService;
import com.pot.zing.framework.common.model.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generates OAuth2 provider authorization URLs for the authorization code flow.
 */
@Tag(name = "OAuth2", description = "OAuth2 authorization URL generation")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
@ConditionalOnProperty(name = "auth.oauth2.enabled", havingValue = "true")
public class OAuth2AuthorizationController {

    private final OAuth2ApplicationService oauth2ApplicationService;

    @Operation(operationId = "getOAuth2AuthorizationUrl", summary = "Generate OAuth2 authorization URL", description = "Returns the provider authorization URL. Redirect the user's browser to this URL to begin the OAuth2 flow. If state is omitted, a random UUID is generated.")
    @GetMapping("/api/v1/oauth2/authorization-url")
    public R<String> getAuthorizationUrl(
            @NotBlank(message = "provider must not be blank") @RequestParam String provider,
            @NotBlank(message = "redirectUri must not be blank") @RequestParam String redirectUri,
            @RequestParam(required = false) String state) {

        return R.success(oauth2ApplicationService.getAuthorizationUrl(provider, redirectUri, state));
    }
}
