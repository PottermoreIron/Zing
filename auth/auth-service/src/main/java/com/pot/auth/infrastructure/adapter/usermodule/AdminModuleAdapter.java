package com.pot.auth.infrastructure.adapter.usermodule;

import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.DeviceDTO;
import com.pot.auth.domain.port.dto.RoleDTO;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.valueobject.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Placeholder adapter for the admin user domain.
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "auth.user-domain.admin.enabled", havingValue = "true")
public class AdminModuleAdapter implements UserModulePort {

    @Override
    public UserDomain supportedDomain() {
        return UserDomain.ADMIN;
    }

    @Override
    public Optional<UserDTO> authenticateWithPassword(String identifier, String password) {
        log.warn("AdminModuleAdapter not implemented: authenticateWithPassword");
        throw new UnsupportedOperationException("Admin domain not yet implemented");
    }

    @Override
    public Optional<UserDTO> findById(UserId userId) {
        log.warn("AdminModuleAdapter not implemented: findById");
        throw new UnsupportedOperationException("Admin domain not yet implemented");
    }

    @Override
    public Optional<UserDTO> findByIdentifier(String identifier) {
        log.warn("AdminModuleAdapter not implemented: findByIdentifier");
        throw new UnsupportedOperationException("Admin domain not yet implemented");
    }

    @Override
    public Optional<UserDTO> findByEmail(String email) {
        log.warn("AdminModuleAdapter not implemented: findByEmail");
        throw new UnsupportedOperationException("Admin domain not yet implemented");
    }

    @Override
    public Optional<UserDTO> findByPhone(String phone) {
        log.warn("AdminModuleAdapter not implemented: findByPhone");
        throw new UnsupportedOperationException("Admin domain not yet implemented");
    }

    @Override
    public UserId createUser(CreateUserCommand command) {
        log.warn("AdminModuleAdapter not implemented: createUser");
        throw new UnsupportedOperationException("Admin domain not yet implemented");
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return false;
    }

    @Override
    public boolean existsByEmail(Email email) {
        return false;
    }

    @Override
    public boolean existsByPhone(Phone phone) {
        return false;
    }

    @Override
    public void updatePassword(UserId userId, Password newPassword) {
        throw new UnsupportedOperationException("Admin domain not yet implemented");
    }

    @Override
    public void lockAccount(UserId userId) {
        throw new UnsupportedOperationException("Admin domain not yet implemented");
    }

    @Override
    public void unlockAccount(UserId userId) {
        throw new UnsupportedOperationException("Admin domain not yet implemented");
    }

    @Override
    public void recordLoginAttempt(UserId userId, boolean success, IpAddress ip, Long timestamp) {
    }

    @Override
    public Set<String> getPermissions(UserId userId) {
        // The admin permission contract is undefined until admin-service is
        // implemented.
        return Collections.emptySet();
    }

    @Override
    public Set<RoleDTO> getRoles(UserId userId) {
        return Collections.emptySet();
    }

    @Override
    public Map<UserId, Set<String>> getPermissionsBatch(List<UserId> userIds) {
        return Collections.emptyMap();
    }

    @Override
    public List<DeviceDTO> getDevices(UserId userId) {
        return Collections.emptyList();
    }

    @Override
    public void recordDeviceLogin(UserId userId, DeviceDTO deviceInfo, IpAddress ip, String refreshToken) {
    }

    @Override
    public void kickDevice(UserId userId, DeviceId deviceId) {
        throw new UnsupportedOperationException("Admin domain not yet implemented");
    }

    @Override
    public void bindOAuth2(UserId userId, String provider, String providerId, Map<String, Object> userInfo) {
        throw new UnsupportedOperationException("Admin domain not yet implemented");
    }

    @Override
    public Optional<UserDTO> findUserByOAuth2(String provider, String openId) {
        log.debug("Admin domain does not support OAuth2 login — provider={}, openId={}", provider, openId);
        return Optional.empty();
    }

    @Override
    public Optional<UserDTO> findUserByWeChat(String weChatOpenId) {
        log.debug("Admin domain does not support WeChat login — weChatOpenId={}", weChatOpenId);
        return Optional.empty();
    }
}
