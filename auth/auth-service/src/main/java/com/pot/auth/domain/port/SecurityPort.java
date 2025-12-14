package com.pot.auth.domain.port;

import java.util.Map;
import java.util.Set;

/**
 * 安全认证端口接口
 *
 * <p>
 * 抽象认证信息的获取，解耦具体的认证框架（Spring Security、Shiro等）
 *
 * <p>
 * 设计模式：端口-适配器模式（Port-Adapter Pattern / Hexagonal Architecture）
 * <ul>
 * <li>Port: 该接口定义领域层需要的安全认证能力</li>
 * <li>Adapter: 由基础设施层提供具体实现（Spring Security Adapter、Shiro Adapter等）</li>
 * </ul>
 *
 * <p>
 * 架构优势：
 * <ul>
 * <li>领域层不依赖具体框架，符合DDD分层原则</li>
 * <li>可通过配置文件切换认证框架，无需修改业务代码</li>
 * <li>易于单元测试，可Mock该接口进行测试</li>
 * <li>新增框架支持只需实现该接口，符合开闭原则</li>
 * </ul>
 *
 * @author pot
 * @since 2025-12-14
 */
public interface SecurityPort {

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，如果未认证返回null
     */
    String getCurrentUserId();

    /**
     * 获取当前用户权限集合
     *
     * <p>
     * 权限格式示例：
     * <ul>
     * <li>资源级权限：user:create, order:delete, product:update</li>
     * <li>功能级权限：system:admin, report:export</li>
     * </ul>
     *
     * @return 权限集合，如果未认证返回空Set（非null）
     */
    Set<String> getCurrentUserPermissions();

    /**
     * 获取当前用户角色集合
     *
     * <p>
     * 角色格式示例：
     * <ul>
     * <li>系统角色：ROLE_ADMIN, ROLE_USER, ROLE_GUEST</li>
     * <li>业务角色：ROLE_MANAGER, ROLE_AUDITOR</li>
     * </ul>
     *
     * @return 角色集合，如果未认证返回空Set（非null）
     */
    Set<String> getCurrentUserRoles();

    /**
     * 判断当前用户是否已认证
     *
     * @return true表示已认证，false表示未认证或匿名用户
     */
    boolean isAuthenticated();

    /**
     * 获取当前用户的所有详细信息
     *
     * <p>
     * 包含用户的扩展信息，如部门、岗位、元数据等
     *
     * @return 用户详细信息Map，如果未认证返回空Map（非null）
     */
    Map<String, Object> getCurrentUserDetails();

    /**
     * 清除当前认证上下文
     *
     * <p>
     * 用于退出登录、切换用户等场景
     */
    void clearContext();
}
