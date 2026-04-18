package com.pot.member.service.infrastructure.client;

import com.pot.zing.framework.common.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

/**
 * Feign client for querying cached permissions from auth-service.
 */
@FeignClient(name = "auth-service", path = "/internal/auth")
public interface AuthServiceClient {

    @GetMapping("/permissions")
    R<Set<String>> getPermissions(
            @RequestParam("userId") Long userId,
            @RequestParam("domain") String domain);
}
