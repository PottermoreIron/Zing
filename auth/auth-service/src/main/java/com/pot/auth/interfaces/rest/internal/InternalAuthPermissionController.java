package com.pot.auth.interfaces.rest.internal;

import com.pot.auth.application.service.PermissionQueryApplicationService;
import com.pot.zing.framework.common.model.R;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Internal REST endpoint exposing cached user permissions to downstream services.
 *
 * <p>This path is <strong>not</strong> exposed through the public gateway; it is only
 * reachable by services on the same internal network (e.g. member-service via Feign).</p>
 */
@Validated
@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class InternalAuthPermissionController {

    private final PermissionQueryApplicationService permissionQueryApplicationService;

    /**
     * Returns the cached permission codes for the given user.
     *
     * @param userId user's business ID
     * @param domain domain context (e.g. "member", "admin")
     */
    @GetMapping("/permissions")
    public R<Set<String>> getPermissions(
            @NotNull @RequestParam("userId") Long userId,
            @NotBlank @RequestParam("domain") String domain) {
        Set<String> permissions = permissionQueryApplicationService.getCachedPermissions(userId, domain);
        return R.success(permissions);
    }
}
