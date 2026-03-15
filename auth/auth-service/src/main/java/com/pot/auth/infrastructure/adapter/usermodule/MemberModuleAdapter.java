package com.pot.auth.infrastructure.adapter.usermodule;

import com.pot.auth.domain.port.UserModulePort;
import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.DeviceDTO;
import com.pot.auth.domain.port.dto.RoleDTO;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.valueobject.*;
import com.pot.auth.infrastructure.client.MemberServiceClient;
import com.pot.member.facade.dto.MemberDTO;
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
 * Member域适配器（防腐层核心实现⭐⭐⭐）
 *
 * <p>
 * <strong>职责</strong>：
 * <ul>
 * <li>实现UserModulePort接口</li>
 * <li>调用MemberServiceClient（Feign）</li>
 * <li>将member-facade的DTO转换成auth领域层DTO（防腐）</li>
 * <li>处理member-service的异常并转换成领域异常</li>
 * </ul>
 *
 * <p>
 * <strong>防腐层价值</strong>：
 * <ul>
 * <li>✅ member-facade的DTO变更不影响auth领域层</li>
 * <li>✅ member-service的API变更只需修改此Adapter</li>
 * <li>✅ 可以轻松Mock此Adapter进行单元测试</li>
 * <li>✅ 符合依赖倒置原则（DIP）</li>
 * </ul>
 *
 * <p>
 * <strong>示例</strong>：
 *
 * <pre>
 * // 领域层使用
 * UserModulePort userPort = userModulePortFactory.getPort(UserDomain.MEMBER);
 * Optional&lt;UserDTO&gt; user = userPort.authenticateWithPassword("john", "password");
 *
 * // Adapter自动处理：
 * // 1. 调用MemberServiceClient
 * // 2. 将MemberDTO转换成领域层UserDTO
 * // 3. 处理异常
 * </pre>
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

    // ========== 用户认证 ==========

    @Override
    public Optional<UserDTO> authenticateWithPassword(String identifier, String password) {
        try {
            // 1. 调用member-service的内部认证API进行密码验证
            R<Boolean> verifyResponse = memberServiceClient.verifyPassword(identifier, password);

            if (verifyResponse == null || !verifyResponse.isSuccess()
                    || !Boolean.TRUE.equals(verifyResponse.getData())) {
                log.debug("密码验证失败: identifier={}", identifier);
                return Optional.empty();
            }

            // 2. 验证成功，查询用户信息
            R<MemberDTO> response = findMemberByIdentifier(identifier);

            if (response == null || !response.isSuccess() || response.getData() == null) {
                log.error("密码验证成功但用户信息查询失败: identifier={}", identifier);
                return Optional.empty();
            }

            // 3. 转换DTO
            return Optional.of(convertToUserDTO(response.getData()));

        } catch (Exception e) {
            log.error("密码认证失败: identifier={}", identifier, e);
            return Optional.empty();
        }
    }

    /**
     * 根据标识符查找用户（支持用户名/邮箱/手机号）
     */
    private R<MemberDTO> findMemberByIdentifier(String identifier) {
        // 1. 判断identifier类型
        if (identifier.contains("@")) {
            // 邮箱
            return memberServiceClient.getMemberByEmail(identifier);
        } else if (identifier.matches("^1[3-9]\\d{9}$")) {
            // 中国手机号
            return memberServiceClient.getMemberByPhone(identifier);
        } else {
            // 用户名
            return memberServiceClient.getMemberByUsername(identifier);
        }
    }

    @Override
    public Optional<UserDTO> findById(UserId userId) {
        try {
            R<MemberDTO> response = memberServiceClient.getMemberById(userId.value());

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
            R<MemberDTO> response = memberServiceClient.getMemberByEmail(email);

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
            R<MemberDTO> response = memberServiceClient.getMemberByPhone(phone);

            if (response == null || !response.isSuccess() || response.getData() == null) {
                return Optional.empty();
            }

            return Optional.of(convertToUserDTO(response.getData()));
        } catch (Exception e) {
            log.error("根据手机号查询用户失败: phone={}", phone, e);
            return Optional.empty();
        }
    }

    // ========== 用户创建 ==========

    @Override
    public UserId createUser(CreateUserCommand command) {
        try {
            // 1. 构建member-facade的CreateMemberRequest
            CreateMemberRequest request = CreateMemberRequest.builder()
                    .nickname(command.username()) // member-facade使用nickname字段
                    .email(command.email() != null ? command.email().value() : null)
                    .phone(command.phone() != null ? command.phone().value() : null)
                    .password(command.password() != null ? command.password().value() : null)
                    .build();

            // 2. 调用member-service创建用户
            R<MemberDTO> response = memberServiceClient.createMember(request);

            if (response == null || !response.isSuccess() || response.getData() == null) {
                String errorMsg = response != null ? response.getMsg() : "创建用户失败";
                throw new UserCreationException(errorMsg);
            }

            // 3. 返回用户ID
            MemberDTO memberDTO = response.getData();
            log.info("用户创建成功: memberId={}, username={}", memberDTO.getMemberId(), memberDTO.getUsername());

            return UserId.of(memberDTO.getMemberId());

        } catch (Exception e) {
            log.error("创建用户失败: username={}", command.username(), e);
            throw new UserCreationException("创建用户失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        try {
            R<MemberDTO> response = memberServiceClient.getMemberByUsername(username);
            return response != null && response.isSuccess() && response.getData() != null;
        } catch (Exception e) {
            log.error("检查用户名是否存在失败: username={}", username, e);
            return false;
        }
    }

    @Override
    public boolean existsByEmail(Email email) {
        try {
            R<Boolean> response = memberServiceClient.checkEmailExists(email.value());
            return response != null && response.isSuccess() && Boolean.TRUE.equals(response.getData());
        } catch (Exception e) {
            log.error("检查邮箱是否存在失败: email={}", email, e);
            return false;
        }
    }

    @Override
    public boolean existsByPhone(Phone phone) {
        try {
            R<Boolean> response = memberServiceClient.checkPhoneExists(phone.value());
            return response != null && response.isSuccess() && Boolean.TRUE.equals(response.getData());
        } catch (Exception e) {
            log.error("检查手机号是否存在失败: phone={}", phone, e);
            return false;
        }
    }

    // ========== 密码管理 ==========

    @Override
    public void updatePassword(UserId userId, Password newPassword) {
        // TODO: 等member-service提供更新密码的内部API
        log.warn("⚠️ TODO: member-service需提供内部API: PUT /internal/member/{}/password", userId);
        throw new UnsupportedOperationException("更新密码功能待member-service提供内部API");
    }

    // ========== 账户管理 ==========

    @Override
    public void lockAccount(UserId userId) {
        try {
            memberServiceClient.lockAccount(userId.value().toString());
            log.info("锁定账户成功: userId={}", userId);
        } catch (Exception e) {
            log.error("锁定账户失败: userId={}", userId, e);
            throw new RuntimeException("锁定账户失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void unlockAccount(UserId userId) {
        try {
            memberServiceClient.unlockAccount(userId.value().toString());
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
                    userId.value().toString(),
                    success,
                    ip != null ? ip.value() : "unknown");
        } catch (Exception e) {
            // 登录尝试记录不阻断主流程
            log.warn("记录登录尝试失败（非关键操作）: userId={}, error={}", userId, e.getMessage());
        }
    }

    // ========== 权限查询 ==========

    @Override
    public Set<String> getPermissions(UserId userId) {
        try {
            R<Set<String>> response = memberServiceClient.getPermissions(userId.value().toString());
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
            R<Set<String>> response = memberServiceClient.getRoles(userId.value().toString());
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData().stream()
                        .map(roleCode -> RoleDTO.builder()
                                .roleCode(roleCode)
                                .roleName(roleCode)
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
            Set<String> userIdStrings = userIds.stream()
                    .map(id -> id.value().toString())
                    .collect(java.util.stream.Collectors.toSet());
            R<Map<String, Set<String>>> response = memberServiceClient.batchQueryPermissions(userIdStrings);
            if (response != null && response.isSuccess() && response.getData() != null) {
                Map<UserId, Set<String>> result = new java.util.HashMap<>();
                response.getData().forEach((idStr, perms) -> {
                    try {
                        result.put(UserId.of(Long.parseLong(idStr)), perms);
                    } catch (NumberFormatException ignore) {
                        log.warn("批量权限查询 userId 格式异常: {}", idStr);
                    }
                });
                return result;
            }
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("批量获取用户权限失败", e);
            return Collections.emptyMap();
        }
    }

    // ========== 设备管理 ==========

    @Override
    public List<DeviceDTO> getDevices(UserId userId) {
        // TODO: 等member-service提供设备列表查询的内部API
        log.warn("⚠️ TODO: member-service需提供内部API: GET /internal/member/{}/devices", userId);
        return Collections.emptyList();
    }

    @Override
    public void recordDeviceLogin(UserId userId, DeviceDTO deviceInfo, IpAddress ip, String refreshToken) {
        // TODO: 等member-service提供���备登录记录的内部API
        log.debug("记录设备登录: userId={}, deviceId={}", userId, deviceInfo.deviceId());
    }

    @Override
    public void kickDevice(UserId userId, DeviceId deviceId) {
        // TODO: 等member-service提供踢出设备的内部API
        log.warn("⚠️ TODO: member-service需提供内部API: DELETE /internal/member/{}/devices/{}", userId, deviceId);
        throw new UnsupportedOperationException("踢出设备功能待member-service提供内部API");
    }

    // ========== OAuth2绑定 ==========

    @Override
    public void bindOAuth2(UserId userId, String provider, String providerId, Map<String, Object> userInfo) {
        try {
            R<Void> response = memberServiceClient.bindOAuth2Account(userId.value(), provider, providerId);

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

    // ========== DTO转换（防腐层核心）==========

    /**
     * 将member-facade的MemberDTO转换成auth领域层的UserDTO
     *
     * <p>
     * 这是防腐层的核心：隔离member-facade的DTO变更
     */
    private UserDTO convertToUserDTO(MemberDTO memberDTO) {
        return UserDTO.builder()
                .userId(UserId.of(memberDTO.getMemberId()))
                .userDomain(UserDomain.MEMBER) // Member域
                .username(memberDTO.getUsername())
                .email(memberDTO.getEmail())
                .phone(memberDTO.getPhone())
                .status(memberDTO.getStatus())
                .emailVerifiedAt(convertToLocalDateTime(memberDTO.getGmtEmailVerifiedAt()))
                .phoneVerifiedAt(convertToLocalDateTime(memberDTO.getGmtPhoneVerifiedAt()))
                .lastLoginAt(null) // MemberDTO中没有此字段，需member-service扩展
                .lastLoginIp(null) // MemberDTO中没有此字段，需member-service扩展
                .build();
    }

    /**
     * 将Unix时间戳（秒）转换为LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(Long timestamp) {
        if (timestamp == null || timestamp <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault());
    }

    // ========== 社交账号查询 ==========

    @Override
    public Optional<UserDTO> findUserByOAuth2(String provider, String openId) {
        try {
            // TODO: 调用 member-service 的社交连接查询 API
            // 目前 member-service 还没有提供根据 OAuth2 信息查询用户的 API
            // 暂时返回空，等待 member-service 实现后再补充
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
            // TODO: 调用 member-service 的社交连接查询 API
            // 目前 member-service 还没有提供根据微信 OpenId 查询用户的 API
            // 暂时返回空，等待 member-service 实现后再补充
            log.debug("查询微信绑定用户: weChatOpenId={}", weChatOpenId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("查询微信绑定用户失败: weChatOpenId={}", weChatOpenId, e);
            return Optional.empty();
        }
    }

    // ========== 异常定义 ==========

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
