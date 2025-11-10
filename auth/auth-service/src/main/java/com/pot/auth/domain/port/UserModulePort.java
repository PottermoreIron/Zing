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

/**
 * 用户模块端口接口（防腐层核心⭐⭐⭐）
 *
 * <p>领域层通过此接口访问用户模块(member/admin-service)，不依赖Feign Client
 * <p>每个用户域都有对应的适配器实现：
 * <ul>
 *   <li>MemberModuleAdapter - 适配member-service</li>
 *   <li>AdminModuleAdapter - 适配admin-service（预留）</li>
 *   <li>MerchantModuleAdapter - 适配merchant-service（未来扩展）</li>
 * </ul>
 *
 * <p>通过UserModulePortFactory根据UserDomain动态获取对应的适配器
 *
 * @author pot
 * @since 1.0.0
 */
public interface UserModulePort {

    /**
     * 标识当前适配器支持的用户域
     */
    UserDomain supportedDomain();

    // ========== 用户认证 ==========

    /**
     * 密码认证
     *
     * @param identifier 用户标识（用户名/邮箱/手机号）
     * @param password   密码明文
     * @return 用户信息（如果认证成功）
     */
    Optional<UserDTO> authenticateWithPassword(String identifier, String password);

    /**
     * 根据ID获取用户信息
     */
    Optional<UserDTO> findById(UserId userId);

    /**
     * 根据标识符获取用户（用户名/邮箱/手机号）
     */
    Optional<UserDTO> findByIdentifier(String identifier);

    /**
     * 根据邮箱获取用户
     */
    Optional<UserDTO> findByEmail(String email);

    /**
     * 根据手机号获取用户
     */
    Optional<UserDTO> findByPhone(String phone);

    // ========== 用户创建 ==========

    /**
     * 创建用户
     *
     * @return 新用户的ID
     */
    UserId createUser(CreateUserCommand command);

    /**
     * 唯一性检查
     */
    boolean existsByUsername(String username);

    boolean existsByEmail(Email email);

    boolean existsByPhone(Phone phone);

    // ========== 密码管理 ==========

    /**
     * 更新密码
     */
    void updatePassword(UserId userId, Password newPassword);

    // ========== 账户管理 ==========

    /**
     * 锁定账户
     */
    void lockAccount(UserId userId);

    /**
     * 解锁账户
     */
    void unlockAccount(UserId userId);

    /**
     * 记录登录尝试
     */
    void recordLoginAttempt(UserId userId, boolean success, IpAddress ip, Long timestamp);

    // ========== 权限查询 ==========

    /**
     * 查询用户权限代码集合
     */
    Set<String> getPermissions(UserId userId);

    /**
     * 查询用户角色
     */
    Set<RoleDTO> getRoles(UserId userId);

    /**
     * 批量查询权限
     */
    Map<UserId, Set<String>> getPermissionsBatch(List<UserId> userIds);

    // ========== 设备管理 ==========

    /**
     * 查询用户设备列表
     */
    List<DeviceDTO> getDevices(UserId userId);

    /**
     * 记录设备登录
     */
    void recordDeviceLogin(UserId userId, DeviceDTO deviceInfo, IpAddress ip, String refreshToken);

    /**
     * 踢出设备
     */
    void kickDevice(UserId userId, DeviceId deviceId);

    // ========== OAuth2绑定 ==========

    /**
     * 通过OAuth2信息查找用户ID
     */
    Optional<UserId> findUserIdByOAuth2(String provider, String providerId);

    /**
     * 绑定OAuth2账号
     */
    void bindOAuth2(UserId userId, String provider, String providerId, Map<String, Object> userInfo);
}

