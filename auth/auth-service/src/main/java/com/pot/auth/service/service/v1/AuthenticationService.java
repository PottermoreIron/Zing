package com.pot.auth.service.service.v1;

import com.pot.auth.service.dto.v1.request.CreateSessionRequest;
import com.pot.auth.service.dto.v1.session.AuthSession;

/**
 * 认证服务接口
 * <p>
 * 定义统一的认证入口，所有认证方式都通过此接口完成
 * <p>
 * 设计原则：
 * 1. 面向接口编程
 * 2. 单一职责 - 只负责认证逻辑
 * 3. 策略模式 - 每种认证方式是一个策略
 * 4. 开闭原则 - 新增认证方式无需修改此接口
 *
 * @author Pot
 * @since 2025-10-25
 */
public interface AuthenticationService {

    /**
     * 执行认证（统一入口）
     * <p>
     * 根据CreateSessionRequest的具体类型，自动调用对应的认证策略
     * 使用多态和策略模式，无需if-else判断
     *
     * @param request 创建会话请求
     * @return 认证会话
     */
    AuthSession authenticate(CreateSessionRequest request);

    /**
     * 刷新会话
     *
     * @param sessionId    会话ID
     * @param refreshToken 刷新令牌
     * @return 新的认证会话
     */
    AuthSession refreshSession(String sessionId, String refreshToken);

    /**
     * 销毁会话（登出）
     *
     * @param sessionId 会话ID
     */
    void destroySession(String sessionId);

    /**
     * 获取会话信息
     *
     * @param sessionId 会话ID
     * @return 认证会话
     */
    AuthSession getSession(String sessionId);

    /**
     * 获取用户的所有会话（多设备管理）
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    java.util.List<AuthSession> listUserSessions(Long userId);

    /**
     * 强制下线指定会话
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     */
    void forceLogoutSession(Long userId, String sessionId);
}


