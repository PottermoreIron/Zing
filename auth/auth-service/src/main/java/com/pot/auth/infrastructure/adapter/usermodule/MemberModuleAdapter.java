package com.pot.auth.infrastructure.adapter.usermodule;

import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.DeviceDTO;
import com.pot.auth.domain.port.dto.RoleDTO;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.valueobject.*;
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
                log.debug("密码验证失败: identifier={}", identifier);
                return Optional.empty();
            }

            return Optional.of(convertToUserDTO(response.getData()));

        } catch (Exception e) {
            log.error("密码认证失败: identifier={}", identifier, e);
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
            log.error("根据ID查询用户失败: userId={}", userId, e);
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
            log.error("根据标识符查询用户失败: identifier={}", identifier, e);
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
            log.error("根据邮箱查询用户失败: email={}", email, e);
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
            log.error("根据手机号查询用户失败: phone={}", phone, e);
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
                String errorMsg = response != null ? response.getMsg() : "创建用户失败";
                throw new UserCreationException(errorMsg);
            }

            MemberDTO memberDTO = response.getData();
            log.info("用户创建成功: memberId={}, nickname={}", memberDTO.getMemberId(), memberDTO.getNickname());

            return UserId.of(memberDTO.getMemberId());

        } catch (Exception e) {
            log.error("创建用户失败: nickname={}", command.nickname(), e);
            throw new UserCreationException("创建用户失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByNickname(String nickname) {
        try {
            R<Boolean> response = memberServiceClient.existsByNickname(nickname);
            return response != null && response.isSuccess() && Boolean.TRUE.equals(response.getData());
        } catch (Exception e) {
            log.error("检查昵称是否存在失败: nickname={}", nickname, e);
            return false;
        }
    }

    @Override
    public boolean existsByEmail(Email email) {
        try {
            R<Boolean> response = memberServiceClient.existsByEmail(email.value());
            return response != null && response.isSuccess() && Boolean.TRUE.equals(response.getData());
        } catch (Exception e) {
            log.error("检查邮箱是否存在失败: email={}", email, e);
            return false;
        }
    }

    @Override
    public boolean existsByPhone(Phone phone) {
        try {
            R<Boolean> response = memberServiceClient.existsByPhone(phone.value());
            return response != null && response.isSuccess() && Boolean.TRUE.equals(response.getData());
        } catch (Exception e) {
            log.error("检查手机号是否存在失败: phone={}", phone, e);
            return false;
        }
    }

    @Override
    public void updatePassword(UserId userId, Password newPassword) {
        try {
            memberServiceClient.updatePassword(userId.value(), newPassword.value());
            log.info("密码更新成功: userId={}", userId);
        } catch (Exception e) {
            log.error("密码更新失败: userId={}", userId, e);
            throw new RuntimeException("密码更新失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void lockAccount(UserId userId) {
        try {
            memberServiceClient.lockAccount(userId.value());
            log.info("锁定账户成功: userId={}", userId);
        } catch (Exception e) {
            log.error("锁定账户失败: userId={}", userId, e);
            throw new RuntimeException("锁定账户失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void unlockAccount(UserId userId) {
        try {
            memberServiceClient.unlockAccount(userId.value());
            log.info("解锁账户成功: userId={}", userId);
        } catch (Exception e) {
            log.error("解锁账户失败: userId={}", userId, e);
            throw new RuntimeException("解锁账户失败: " + e.getMessage(), e);
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
            log.warn("记录登录尝试失败（非关键操作）: userId={}, error={}", userId, e.getMessage());
        }
    }

    @Override
    public Set<String> getPermissions(UserId userId) {
        try {
            R<Set<String>> response = memberServiceClient.getPermissions(userId.value());
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData();
            }
            log.warn("获取权限返回空结果: userId={}", userId);
            return Collections.emptySet();
        } catch (Exception e) {
            log.error("获取用户权限失败: userId={}", userId, e);
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
                                .roleCode(r.getRoleCode())
                                .roleName(r.getRoleName())
                                .build())
                        .collect(java.util.stream.Collectors.toSet());
            }
            return Collections.emptySet();
        } catch (Exception e) {
            log.error("获取用户角色失败: userId={}", userId, e);
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
            log.error("批量获取用户权限失败", e);
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
                                .deviceId(d.getDeviceToken() != null ? DeviceId.of(d.getDeviceToken()) : null)
                                .deviceType(d.getDeviceType())
                                .build())
                        .collect(java.util.stream.Collectors.toList());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("获取设备列表失败（非关键操作）: userId={}, error={}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void recordDeviceLogin(UserId userId, DeviceDTO deviceInfo, IpAddress ip, String refreshToken) {
        // Member service does not expose an internal device-login recording API yet.
        log.debug("记录设备登录: userId={}, deviceId={}", userId, deviceInfo.deviceId());
    }

    @Override
    public void kickDevice(UserId userId, DeviceId deviceId) {
        try {
            memberServiceClient.kickDevice(userId.value(), deviceId.value());
            log.info("踢出设备成功: userId={}, deviceId={}", userId, deviceId);
        } catch (Exception e) {
            log.error("踢出设备失败: userId={}, deviceId={}", userId, deviceId, e);
            throw new RuntimeException("踢出设备失败: " + e.getMessage(), e);
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
                String errorMsg = response != null ? response.getMsg() : "绑定OAuth2账号失败";
                throw new OAuth2BindException(errorMsg);
            }

            log.info("OAuth2账号绑定成功: userId={}, provider={}, providerId={}", userId, provider, providerId);
        } catch (Exception e) {
            log.error("绑定OAuth2账号失败: userId={}, provider={}", userId, provider, e);
            throw new OAuth2BindException("绑定OAuth2账号失败: " + e.getMessage(), e);
        }
    }

    /**
     * Converts the member facade DTO into the auth-domain user DTO.
     */
    private UserDTO convertToUserDTO(MemberDTO memberDTO) {
        return UserDTO.builder()
                .userId(UserId.of(memberDTO.getMemberId()))
                .userDomain(UserDomain.MEMBER)
                .nickname(memberDTO.getNickname())
                .email(memberDTO.getEmail())
                .phone(memberDTO.getPhone())
                .status(memberDTO.getStatus())
                .emailVerifiedAt(null)
                .phoneVerifiedAt(null)
                .lastLoginAt(convertToLocalDateTime(memberDTO.getGmtLastLoginAt()))
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
            log.debug("查询OAuth2绑定用户: provider={}, openId={}", provider, openId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("查询OAuth2绑定用户失败: provider={}, openId={}", provider, openId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserDTO> findUserByWeChat(String weChatOpenId) {
        try {
            // Member service does not expose WeChat social-connection lookup yet.
            log.debug("查询微信绑定用户: weChatOpenId={}", weChatOpenId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("查询微信绑定用户失败: weChatOpenId={}", weChatOpenId, e);
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
