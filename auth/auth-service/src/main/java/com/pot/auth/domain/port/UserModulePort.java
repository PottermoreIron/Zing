package com.pot.auth.domain.port;

import com.pot.auth.domain.port.dto.CreateUserCommand;
import com.pot.auth.domain.port.dto.DeviceDTO;
import com.pot.auth.domain.port.dto.RoleDTO;
import com.pot.auth.domain.port.dto.UserDTO;
import com.pot.auth.domain.shared.valueobject.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface UserModulePort {

        UserDomain supportedDomain();


        Optional<UserDTO> authenticateWithPassword(String identifier, String password);

        Optional<UserDTO> findById(UserId userId);

        Optional<UserDTO> findByIdentifier(String identifier);

        Optional<UserDTO> findByEmail(String email);

        Optional<UserDTO> findByPhone(String phone);


        UserId createUser(CreateUserCommand command);

        boolean existsByNickname(String nickname);

    boolean existsByEmail(Email email);

    boolean existsByPhone(Phone phone);


        void updatePassword(UserId userId, Password newPassword);


        void lockAccount(UserId userId);

        void unlockAccount(UserId userId);

        void recordLoginAttempt(UserId userId, boolean success, IpAddress ip, Long timestamp);


        Set<String> getPermissions(UserId userId);

        Set<RoleDTO> getRoles(UserId userId);

        Map<UserId, Set<String>> getPermissionsBatch(List<UserId> userIds);


        List<DeviceDTO> getDevices(UserId userId);

        void recordDeviceLogin(UserId userId, DeviceDTO deviceInfo, IpAddress ip, String refreshToken);

        void kickDevice(UserId userId, DeviceId deviceId);


        Optional<UserDTO> findUserByOAuth2(String provider, String openId);

        Optional<UserDTO> findUserByWeChat(String weChatOpenId);

        void bindOAuth2(UserId userId, String provider, String providerId, Map<String, Object> userInfo);
}
