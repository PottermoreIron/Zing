package com.pot.member.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pot.member.facade.dto.request.BindSocialAccountRequest;
import com.pot.member.service.entity.SocialConnection;

import java.util.List;
import java.util.Map;

/**
 * 社交账号连接服务接口
 * <p>
 * 提供社交账号绑定、解绑、查询、管理等完整业务功能
 * </p>
 *
 * @author Pot
 * @since 2025-09-01 23:25:59
 */
public interface SocialConnectionsService extends IService<SocialConnection> {

    /**
     * 创建社交账号连接
     * <p>
     * 业务规则：
     * 1. 验证用户是否存在
     * 2. 检查是否已绑定该平台（不允许重复绑定）
     * 3. 检查第三方账号是否被其他用户绑定
     * 4. 创建连接记录并设置为活跃状态
     * </p>
     *
     * @param request 绑定请求
     * @return 创建的连接实体
     * @throws com.pot.zing.framework.common.excption.BusinessException 业务异常
     */
    SocialConnection createConnection(BindSocialAccountRequest request);

    /**
     * 删除社交账号连接（软删除）
     * <p>
     * 业务规则：
     * 1. 验证连接是否存在
     * 2. 检查是否可以解绑（至少保留一种登录方式）
     * 3. 软删除记录（设置gmtDeletedAt）
     * </p>
     *
     * @param memberId 用户ID
     * @param provider 平台提供商
     * @throws com.pot.zing.framework.common.excption.BusinessException 业务异常
     */
    void removeConnection(Long memberId, String provider);

    /**
     * 查询用户的所有活跃连接
     * <p>
     * 只返回未删除且活跃的连接
     * </p>
     *
     * @param memberId 用户ID
     * @return 连接列表
     */
    List<SocialConnection> listByMemberId(Long memberId);

    /**
     * 查询特定平台的连接
     *
     * @param memberId 用户ID
     * @param provider 平台提供商
     * @return 连接实体，未找到返回null
     */
    SocialConnection getByMemberIdAndProvider(Long memberId, String provider);

    /**
     * 根据第三方账号查询连接
     * <p>
     * 用于检查第三方账号是否已被绑定
     * </p>
     *
     * @param provider         平台提供商
     * @param providerMemberId 第三方平台用户ID
     * @return 连接实体，未找到返回null
     */
    SocialConnection getByProviderAndProviderId(String provider, String providerMemberId);

    /**
     * 更新令牌信息
     * <p>
     * 用于刷新过期的访问令牌
     * </p>
     *
     * @param memberId     用户ID
     * @param provider     平台提供商
     * @param accessToken  新的访问令牌
     * @param refreshToken 新的刷新令牌（可选）
     * @param expiresAt    过期时间（Unix时间戳，可选）
     */
    void updateTokens(Long memberId, String provider, String accessToken,
                      String refreshToken, Long expiresAt);

    /**
     * 设置主社交账号
     * <p>
     * 业务规则：
     * 1. 取消该用户其他连接的主账号标记
     * 2. 设置指定连接为主账号
     * </p>
     *
     * @param memberId 用户ID
     * @param provider 平台提供商
     */
    void setPrimary(Long memberId, String provider);

    /**
     * 检查用户是否可以解绑
     * <p>
     * 业务规则：至少保留一种登录方式
     * 检查项：
     * 1. 绑定的社交账号数量
     * 2. 是否设置了密码
     * 3. 是否绑定了手机号/邮箱
     * </p>
     *
     * @param memberId 用户ID
     * @return true-可以解绑，false-不可以（只剩最后一种登录方式）
     */
    boolean canUnbind(Long memberId);

    /**
     * 验证绑定请求
     * <p>
     * 验证项：
     * 1. 用户是否存在
     * 2. 是否已绑定该平台
     * 3. 第三方账号是否被其他用户绑定
     * </p>
     *
     * @param request 绑定请求
     * @throws com.pot.zing.framework.common.excption.BusinessException 验证失败
     */
    void validateBindRequest(BindSocialAccountRequest request);

    /**
     * 更新最后使用时间
     * <p>
     * 用于记录社交账号的活跃度
     * </p>
     *
     * @param memberId 用户ID
     * @param provider 平台提供商
     */
    void updateLastUsedTime(Long memberId, String provider);

    /**
     * 批量查询用户的社交连接
     * <p>
     * 用于性能优化，一次查询多个用户的连接
     * </p>
     *
     * @param memberIds 用户ID列表
     * @return 用户ID到连接列表的映射
     */
    Map<Long, List<SocialConnection>> batchGetByMemberIds(List<Long> memberIds);

    /**
     * 停用社交连接
     * <p>
     * 将连接设置为非活跃状态，但不删除
     * </p>
     *
     * @param memberId 用户ID
     * @param provider 平台提供商
     */
    void deactivateConnection(Long memberId, String provider);

    /**
     * 激活社交连接
     * <p>
     * 将连接设置为活跃状态
     * </p>
     *
     * @param memberId 用户ID
     * @param provider 平台提供商
     */
    void activateConnection(Long memberId, String provider);
}
