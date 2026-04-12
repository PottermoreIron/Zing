package com.pot.auth.infrastructure.adapter.usermodule;

import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.DeviceDTO;
import com.pot.auth.domain.port.dto.RoleDTO;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.valueobject.*;
import com.pot.auth.infrastructure.exception.AuthInfrastructureException;
import com.pot.auth.infrastructure.client.MemberServiceClient;
import com.pot.member.facade.dto.MemberDTO;
import com.pot.member.facade.dto.request.BindSocialAccountRequest;
import com.pot.member.facade.dto.request.CreateMemberRequest;
import com.pot.zing.framework.common.model.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Anti-corruption adapter between auth and member-service contracts.
 *
 * @author pot
 * @since 2025-12-14
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "auth.user-domain.member.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class MemberModuleAdapter implements UserModulePort {

    private final MemberServiceClient memberServiceClient;

    @Override
    public UserDomain supportedDomain() {
        return UserDomain.MEMBER;
    }

    @Override
    public Optional<UserDTO> authenticateWithPassword(String identifier, String password) {
        try {
            R<MemberDTO> response = memberServiceClient.authenticateWithPassword(identifier, password);

            if (response == null || !response.isSuccess() || response.getData() == null) {
                log.debug("Password verification failed — identifier={}", identifier);
                return Optional.empty();
            }

            return Optional.of(convertToUserDTO(response.getData()));

        } catch (Exception e) {
            log.error("Password authentication error — identifier={}", identifier, e);
            return Optional.empty();
        }
    }

    /**
     * Resolves a member lookup endpoint from the identifier format.
     */
    private R<MemberDTO> findMemberByIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return memberServiceClient.findByEmail(identifier);
        } else if (identifier.matches("^1[3-9]\\d{9}$")) {
            return memberServiceClient.findByPhone(identifier);
        } else {
            return memberServiceClient.findByNickname(identifier);
        }
    }

    @Override
    public Optional<UserDTO> findById(UserId userId) {
        try {
            R<MemberDTO> response = memberServiceClient.findById(userId.value());

            if (response == null || !response.isSuccess() || response.getData() == null) {
                return Optional.empty();
            }

            return Optional.of(convertToUserDTO(response.getData()));
        } catch (Exception e) {
            log.error("Failed to query user by ID — userId={}", userId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserDTO> findByIdentifier(String identifier) {
        try {
            R<MemberDTO> response = findMemberByIdentifier(identifier);

            if (response == null || !response.isSuccess() || response.getData() == null) {
                return Optional.empty();
            }

            return Optional.of(convertToUserDTO(response.getData()));
        } catch (Exception e) {
            log.error("Failed to query user by identifier — identifier={}", identifier, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserDTO> findByEmail(String email) {
        try {
            R<MemberDTO> response = memberServiceClient.findByEmail(email);

            if (response == null || !response.isSuccess() || response.getData() == null) {
                return Optional.empty();
            }

            return Optional.of(convertToUserDTO(response.getData()));
        } catch (Exception e) {
            log.error("Failed to query user by email — email={}", email, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserDTO> findByPhone(String phone) {
        try {
            R<MemberDTO> response = memberServiceClient.findByPhone(phone);

            if (response == null || !response.isSuccess() || response.getData() == null) {
                return Optional.empty();
            }

            return Optional.of(convertToUserDTO(response.getData()));
        } catch (Exception e) {
            log.error("Failed to query user by phone — phone={}", phone, e);
            return Optional.empty();
        }
    }

    @Override
    public UserId createUser(CreateUserCommand command) {
        try {
            CreateMemberRequest request = CreateMemberRequest.builder()
                    .nickname(command.nickname())
                    .email(command.email() != null ? command.email().value() : null)
                    .phone(command.phone() != null ? command.phone().value() : null)
                    .password(command.password() != null ? command.password().value() : null)
                    .build();

            R<MemberDTO> response = memberServiceClient.createMember(request);

            if (response == null || !response.isSuccess() || response.getData() == null) {
                String errorMsg = response != null ? response.getMsg() : "User creation failed";
                throw new UserCreationException(errorMsg);
            }

            MemberDTO memberDTO = response.getData();
            log.info("User created — memberId={}, nickname={}", memberDTO.memberId(), memberDTO.nickname());

            return UserId.of(memberDTO.memberId());

        } catch (UserCreationException e) {
            throw e;
        } catch (Exception e) {
            log.error("User creation failed — nickname={}", command.nickname(), e);
            throw new UserCreationException("User creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByNickname(String nickname) {
        try {
            R<Boolean> response = memberServiceClient.existsByNickname(nickname);
            return response != null && response.isSuccess() && Boolean.TRUE.equals(response.getData());
        } catch (Exception e) {
            log.error("Failed to check nickname existence — nickname={}", nickname, e);
            return false;
        }
    }

    @Override
    public boolean existsByEmail(Email email) {
        try {
            R<Boolean> response = memberServiceClient.existsByEmail(email.value());
            return response != null && response.isSuccess() && Boolean.TRUE.equals(response.getData());
        } catch (Exception e) {
            log.error("Failed to check email existence — email={}", email, e);
            return false;
        }
    }

    @Override
    public boolean existsByPhone(Phone phone) {
        try {
            R<Boolean> response = memberServiceClient.existsByPhone(phone.value());
            return response != null && response.isSuccess() && Boolean.TRUE.equals(response.getData());
        } catch (Exception e) {
            log.error("Failed to check phone existence — phone={}", phone, e);
            return false;
        }
    }

    @Override
    public void updatePassword(UserId userId, Password newPassword) {
        try {
            memberServiceClient.updatePassword(userId.value(), newPassword.value());
            log.info("Password updated — userId={}", userId);
        } catch (Exception e) {
            log.error("Password update failed — userId={}", userId, e);
            throw new AuthInfrastructureException("Password update failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void lockAccount(UserId userId) {
        try {
            memberServiceClient.lockAccount(userId.value());
            log.info("Account locked — userId={}", userId);
        } catch (Exception e) {
            log.error("Failed to lock account — userId={}", userId, e);
            throw new AuthInfrastructureException("Failed to lock account: " + e.getMessage(), e);
        }
    }

    @Override
    public void unlockAccount(UserId userId) {
        try {
            memberServiceClient.unlockAccount(userId.value());
            log.info("Account unlocked — userId={}", userId);
        } catch (Exception e) {
            log.error("Failed to unlock account — userId={}", userId, e);
            throw new AuthInfrastructureException("Failed to unlock account: " + e.getMessage(), e);
        }
    }

    @Override
    public void recordLoginAttempt(UserId userId, boolean success, IpAddress ip, Long timestamp) {
        try {
            memberServiceClient.recordLoginAttempt(
                    userId.value(),
                    success,
                    ip != null ? ip.value() : "unknown",
                    timestamp);
        } catch (Exception e) {
            // Login-attempt tracking is best-effort and must not block auth flows.
            log.warn("Failed to record login attempt (non-critical) — userId={}, error={}", userId, e.getMessage());
        }
    }

    @Override
    public Set<String> getPermissions(UserId userId) {
        try {
            R<Set<String>> response = memberServiceClient.getPermissions(userId.value());
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData();
            }
            log.warn("Permission query returned empty result — userId={}", userId);
            return Collections.emptySet();
        } catch (Exception e) {
            log.error("Failed to fetch user permissions — userId={}", userId, e);
            return Collections.emptySet();
        }
    }

    @Override
    public Set<RoleDTO> getRoles(UserId userId) {
        try {
            R<List<com.pot.member.facade.dto.RoleDTO>> response = memberServiceClient.getRoles(userId.value());
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData().stream()
                        .map(r -> RoleDTO.builder()
                                .roleCode(r.roleCode())
                                .roleName(r.roleName())
                                .build())
                        .collect(java.util.stream.Collectors.toSet());
            }
            return Collections.emptySet();
        } catch (Exception e) {
            log.error("Failed to fetch user roles — userId={}", userId, e);
            return Collections.emptySet();
        }
    }

    @Override
    public Map<UserId, Set<String>> getPermissionsBatch(List<UserId> userIds) {
        try {
            List<Long> idList = userIds.stream()
                    .map(UserId::value)
                    .collect(java.util.stream.Collectors.toList());
            R<Map<Long, Set<String>>> response = memberServiceClient.getPermissionsBatch(idList);
            if (response != null && response.isSuccess() && response.getData() != null) {
                Map<UserId, Set<String>> result = new java.util.HashMap<>();
                response.getData().forEach((id, perms) -> result.put(UserId.of(id), perms));
                return result;
            }
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Failed to batch-fetch user permissions", e);
            return Collections.emptyMap();
        }
    }

    @Override
    public List<DeviceDTO> getDevices(UserId userId) {
        try {
            R<List<com.pot.member.facade.dto.DeviceDTO>> response = memberServiceClient.getDevices(userId.value());
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData().stream()
                        .map(d -> DeviceDTO.builder()
                                .deviceId(d.deviceToken() != null ? DeviceId.of(d.deviceToken()) : null)
                                .deviceType(d.deviceType())
                                .build())
                        .collect(java.util.stream.Collectors.toList());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to retrieve device list (non-critical) — userId={}, error={}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void recordDeviceLogin(UserId userId, DeviceDTO deviceInfo, IpAddress ip, String refreshToken) {
        // Member service does not expose an internal device-login recording API yet.
        log.debug("Device login recorded — userId={}, deviceId={}", userId, deviceInfo.deviceId());
    }

    @Override
    public void kickDevice(UserId userId, DeviceId deviceId) {
        try {
            memberServiceClient.kickDevice(userId.value(), deviceId.value());
            log.info("Device kicked — userId={}, deviceId={}", userId, deviceId);
        } catch (Exception e) {
            log.error("Failed to kick device — userId={}, deviceId={}", userId, deviceId, e);
            throw new AuthInfrastructureException("Failed to kick device: " + e.getMessage(), e);
        }
    }

    @Override
    public void bindOAuth2(UserId userId, String provider, String providerId, Map<String, Object> userInfo) {
        try {
            BindSocialAccountRequest request = BindSocialAccountRequest.builder()
                    .memberId(userId.value())
                    .provider(provider)
                    .providerMemberId(providerId)
                    .build();
            R<Void> response = memberServiceClient.bindOAuth2(userId.value(), request);

            if (response == null || !response.isSuccess()) {
                String errorMsg = response != null ? response.getMsg() : "OAuth2 account binding failed";
                throw new OAuth2BindException(errorMsg);
            }

            log.info("OAuth2 account bound — userId={}, provider={}, providerId={}", userId, provider, providerId);
        } catch (OAuth2BindException e) {
            throw e;
        } catch (Exception e) {
            log.error("OAuth2 account binding failed: userId={}, provider={}", userId, provider, e);
            throw new OAuth2BindException("OAuth2 account binding failed: " + e.getMessage(), e);
        }
    }

    /**
     * Converts the member facade DTO into the auth-domain user DTO.
     */
    private UserDTO convertToUserDTO(MemberDTO memberDTO) {
        return UserDTO.builder()
                .userId(UserId.of(memberDTO.memberId()))
                .userDomain(UserDomain.MEMBER)
                .nickname(memberDTO.nickname())
                .email(memberDTO.email())
                .phone(memberDTO.phone())
                .status(memberDTO.status())
                .emailVerifiedAt(null)
                .phoneVerifiedAt(null)
                .lastLoginAt(convertToLocalDateTime(memberDTO.gmtLastLoginAt()))
                .lastLoginIp(null)
                .build();
    }

    /**
     * Converts a Unix timestamp in seconds to a local date-time.
     */
    private LocalDateTime convertToLocalDateTime(Long timestamp) {
        if (timestamp == null || timestamp <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault());
    }

    @Override
    public Optional<UserDTO> findUserByOAuth2(String provider, String openId) {
        try {
            // Member service does not expose OAuth2 social-connection lookup yet.
            log.debug("Querying OAuth2-bound user — provider={}, openId={}", provider, openId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to query OAuth2-bound user — provider={}, openId={}", provider, openId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserDTO> findUserByWeChat(String weChatOpenId) {
        try {
            // Member service does not expose WeChat social-connection lookup yet.
            log.debug("Querying WeChat-bound user — weChatOpenId={}", weChatOpenId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to query WeChat-bound user — weChatOpenId={}", weChatOpenId, e);
            return Optional.empty();
        }
    }

    public static class UserCreationException extends RuntimeException {
        public UserCreationException(String message) {
            super(message);
        }

        public UserCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class OAuth2BindException extends RuntimeException {
        public OAuth2BindException(String message) {
            super(message);
        }

        public OAuth2BindException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
