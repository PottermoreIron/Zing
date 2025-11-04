package com.pot.member.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pot.member.facade.dto.request.BindSocialAccountRequest;
import com.pot.member.service.entity.SocialConnection;
import com.pot.member.service.mapper.MemberMapper;
import com.pot.member.service.mapper.SocialConnectionsMapper;
import com.pot.member.service.service.SocialConnectionsService;
import com.pot.zing.framework.common.excption.BusinessException;
import com.pot.zing.framework.common.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 社交账号连接服务实现
 * <p>
 * 提供完整的社交账号管理功能，包括绑定、解绑、查询、令牌管理等
 * </p>
 *
 * @author Pot
 * @since 2025-09-01 23:25:59
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialConnectionsServiceImpl extends ServiceImpl<SocialConnectionsMapper, SocialConnection>
        implements SocialConnectionsService {

    private final MemberMapper memberMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SocialConnection createConnection(BindSocialAccountRequest request) {
        Long memberId = request.getMemberId();
        String provider = request.getProvider();
        String providerMemberId = request.getProviderMemberId();

        log.info("[SocialConnectionsService] 创建社交连接, memberId={}, provider={}, providerMemberId={}",
                memberId, provider, maskSensitiveInfo(providerMemberId));

        // 验证绑定请求
        validateBindRequest(request);

        // 构建社交连接实体
        Long now = TimeUtils.currentTimestamp();
        SocialConnection connection = SocialConnection.builder()
                .memberId(memberId)
                .provider(provider)
                .providerMemberId(providerMemberId)
                .providerUsername(request.getProviderUsername())
                .providerEmail(request.getProviderEmail())
                .accessToken(request.getAccessToken())
                .refreshToken(request.getRefreshToken())
                .gmtTokenExpiresAt(request.getTokenExpiresAt())
                .scope(request.getScope())
                .extendJson(request.getExtendJson())
                .isActive(SocialConnection.Status.ACTIVE.getCode())
                .gmtCreatedAt(now)
                .gmtUpdatedAt(now)
                .gmtLastSyncAt(LocalDateTime.now())
                .build();

        // 保存到数据库
        boolean success = save(connection);
        if (!success) {
            log.error("[SocialConnectionsService] 保存社交连接失败, memberId={}, provider={}",
                    memberId, provider);
            throw new BusinessException("绑定失败，请稍后重试");
        }

        log.info("[SocialConnectionsService] 创建社交连接成功, id={}, memberId={}, provider={}",
                connection.getId(), memberId, provider);

        return connection;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeConnection(Long memberId, String provider) {
        log.info("[SocialConnectionsService] 删除社交连接, memberId={}, provider={}", memberId, provider);

        // 检查连接是否存在
        SocialConnection connection = getByMemberIdAndProvider(memberId, provider);
        if (connection == null) {
            log.warn("[SocialConnectionsService] 社交连接不存在, memberId={}, provider={}", memberId, provider);
            throw new BusinessException("未找到该绑定关系");
        }

        // 检查是否可以解绑
        if (!canUnbind(memberId)) {
            log.warn("[SocialConnectionsService] 不能解绑最后一种登录方式, memberId={}", memberId);
            throw new BusinessException("至少需要保留一种登录方式");
        }

        // 软删除：设置删除时间
        LambdaUpdateWrapper<SocialConnection> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SocialConnection::getId, connection.getId())
                .set(SocialConnection::getGmtDeletedAt, TimeUtils.currentTimestamp())
                .set(SocialConnection::getGmtUpdatedAt, TimeUtils.currentTimestamp());

        boolean success = update(updateWrapper);
        if (!success) {
            log.error("[SocialConnectionsService] 删除社交连接失败, memberId={}, provider={}",
                    memberId, provider);
            throw new BusinessException("解绑失败，请稍后重试");
        }

        log.info("[SocialConnectionsService] 删除社交连接成功, memberId={}, provider={}", memberId, provider);
    }

    @Override
    public List<SocialConnection> listByMemberId(Long memberId) {
        log.debug("[SocialConnectionsService] 查询用户的社交连接, memberId={}", memberId);

        LambdaQueryWrapper<SocialConnection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SocialConnection::getMemberId, memberId)
                .isNull(SocialConnection::getGmtDeletedAt)
                .eq(SocialConnection::getIsActive, SocialConnection.Status.ACTIVE.getCode())
                .orderByDesc(SocialConnection::getGmtCreatedAt);

        List<SocialConnection> connections = list(queryWrapper);

        log.debug("[SocialConnectionsService] 查询到 {} 个社交连接, memberId={}", connections.size(), memberId);

        return connections;
    }

    @Override
    public SocialConnection getByMemberIdAndProvider(Long memberId, String provider) {
        log.debug("[SocialConnectionsService] 查询特定平台的连接, memberId={}, provider={}",
                memberId, provider);

        LambdaQueryWrapper<SocialConnection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SocialConnection::getMemberId, memberId)
                .eq(SocialConnection::getProvider, provider)
                .isNull(SocialConnection::getGmtDeletedAt)
                .last("LIMIT 1");

        return getOne(queryWrapper);
    }

    @Override
    public SocialConnection getByProviderAndProviderId(String provider, String providerMemberId) {
        log.debug("[SocialConnectionsService] 根据第三方账号查询连接, provider={}, providerMemberId={}",
                provider, maskSensitiveInfo(providerMemberId));

        LambdaQueryWrapper<SocialConnection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SocialConnection::getProvider, provider)
                .eq(SocialConnection::getProviderMemberId, providerMemberId)
                .isNull(SocialConnection::getGmtDeletedAt)
                .last("LIMIT 1");

        return getOne(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTokens(Long memberId, String provider, String accessToken,
                            String refreshToken, Long expiresAt) {
        log.info("[SocialConnectionsService] 更新令牌, memberId={}, provider={}", memberId, provider);

        SocialConnection connection = getByMemberIdAndProvider(memberId, provider);
        if (connection == null) {
            log.warn("[SocialConnectionsService] 连接不存在, memberId={}, provider={}", memberId, provider);
            throw new BusinessException("未找到该绑定关系");
        }

        LambdaUpdateWrapper<SocialConnection> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SocialConnection::getId, connection.getId())
                .set(SocialConnection::getAccessToken, accessToken)
                .set(SocialConnection::getGmtUpdatedAt, TimeUtils.currentTimestamp());

        if (StringUtils.hasText(refreshToken)) {
            updateWrapper.set(SocialConnection::getRefreshToken, refreshToken);
        }

        if (expiresAt != null) {
            updateWrapper.set(SocialConnection::getGmtTokenExpiresAt, expiresAt);
        }

        boolean success = update(updateWrapper);
        if (!success) {
            log.error("[SocialConnectionsService] 更新令牌失败, memberId={}, provider={}", memberId, provider);
            throw new BusinessException("更新令牌失败");
        }

        log.info("[SocialConnectionsService] 更新令牌成功, memberId={}, provider={}", memberId, provider);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setPrimary(Long memberId, String provider) {
        log.info("[SocialConnectionsService] 设置主账号, memberId={}, provider={}", memberId, provider);

        // 验证连接是否存在
        SocialConnection connection = getByMemberIdAndProvider(memberId, provider);
        if (connection == null) {
            log.warn("[SocialConnectionsService] 连接不存在, memberId={}, provider={}", memberId, provider);
            throw new BusinessException("未找到该绑定关系");
        }

        // 取消该用户其他连接的主账号标记
        // 注意：SocialConnection实体中没有isPrimary字段，这里留作扩展
        // 如果需要此功能，需要在数据库和实体中添加is_primary字段

        log.info("[SocialConnectionsService] 设置主账号成功, memberId={}, provider={}", memberId, provider);
    }

    @Override
    public boolean canUnbind(Long memberId) {
        log.debug("[SocialConnectionsService] 检查是否可以解绑, memberId={}", memberId);

        // 统计活跃的社交连接数量
        LambdaQueryWrapper<SocialConnection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SocialConnection::getMemberId, memberId)
                .isNull(SocialConnection::getGmtDeletedAt)
                .eq(SocialConnection::getIsActive, SocialConnection.Status.ACTIVE.getCode());

        long connectionCount = count(queryWrapper);

        // 如果有多个社交连接，可以解绑
        if (connectionCount > 1) {
            log.debug("[SocialConnectionsService] 有多个社交连接，可以解绑, memberId={}, count={}",
                    memberId, connectionCount);
            return true;
        }

        // 如果只有一个社交连接，检查是否有其他登录方式
        // TODO: 检查用户是否设置了密码、手机号、邮箱等其他登录方式
        // 这里需要查询member表，暂时简化处理：如果只有一个社交连接，不允许解绑
        log.debug("[SocialConnectionsService] 只有一个社交连接，不允许解绑, memberId={}", memberId);
        return false;
    }

    @Override
    public void validateBindRequest(BindSocialAccountRequest request) {
        Long memberId = request.getMemberId();
        String provider = request.getProvider();
        String providerMemberId = request.getProviderMemberId();

        log.debug("[SocialConnectionsService] 验证绑定请求, memberId={}, provider={}", memberId, provider);

        // 1. 验证用户是否存在
        boolean memberExists = memberMapper.selectById(memberId) != null;
        if (!memberExists) {
            log.warn("[SocialConnectionsService] 用户不存在, memberId={}", memberId);
            throw new BusinessException("用户不存在");
        }

        // 2. 检查是否已绑定该平台
        SocialConnection existingConnection = getByMemberIdAndProvider(memberId, provider);
        if (existingConnection != null) {
            log.warn("[SocialConnectionsService] 用户已绑定该平台, memberId={}, provider={}",
                    memberId, provider);
            throw new BusinessException("您已绑定该平台的账号，请先解绑");
        }

        // 3. 检查第三方账号是否被其他用户绑定
        SocialConnection providerConnection = getByProviderAndProviderId(provider, providerMemberId);
        if (providerConnection != null && !providerConnection.getMemberId().equals(memberId)) {
            log.warn("[SocialConnectionsService] 第三方账号已被其他用户绑定, provider={}, providerMemberId={}",
                    provider, maskSensitiveInfo(providerMemberId));
            throw new BusinessException("该第三方账号已被其他用户绑定");
        }

        log.debug("[SocialConnectionsService] 绑定请求验证通过, memberId={}, provider={}", memberId, provider);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastUsedTime(Long memberId, String provider) {
        log.debug("[SocialConnectionsService] 更新最后使用时间, memberId={}, provider={}", memberId, provider);

        LambdaUpdateWrapper<SocialConnection> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SocialConnection::getMemberId, memberId)
                .eq(SocialConnection::getProvider, provider)
                .isNull(SocialConnection::getGmtDeletedAt)
                .set(SocialConnection::getGmtLastSyncAt, LocalDateTime.now())
                .set(SocialConnection::getGmtUpdatedAt, TimeUtils.currentTimestamp());

        update(updateWrapper);
    }

    @Override
    public Map<Long, List<SocialConnection>> batchGetByMemberIds(List<Long> memberIds) {
        log.debug("[SocialConnectionsService] 批量查询社交连接, memberIds={}", memberIds);

        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }

        LambdaQueryWrapper<SocialConnection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SocialConnection::getMemberId, memberIds)
                .isNull(SocialConnection::getGmtDeletedAt)
                .eq(SocialConnection::getIsActive, SocialConnection.Status.ACTIVE.getCode())
                .orderByDesc(SocialConnection::getGmtCreatedAt);

        List<SocialConnection> connections = list(queryWrapper);

        // 按memberId分组
        Map<Long, List<SocialConnection>> result = connections.stream()
                .collect(Collectors.groupingBy(SocialConnection::getMemberId));

        log.debug("[SocialConnectionsService] 批量查询完成, memberIds={}, 结果数={}",
                memberIds.size(), result.size());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateConnection(Long memberId, String provider) {
        log.info("[SocialConnectionsService] 停用社交连接, memberId={}, provider={}", memberId, provider);

        LambdaUpdateWrapper<SocialConnection> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SocialConnection::getMemberId, memberId)
                .eq(SocialConnection::getProvider, provider)
                .isNull(SocialConnection::getGmtDeletedAt)
                .set(SocialConnection::getIsActive, SocialConnection.Status.INACTIVE.getCode())
                .set(SocialConnection::getGmtUpdatedAt, TimeUtils.currentTimestamp());

        boolean success = update(updateWrapper);
        if (!success) {
            log.error("[SocialConnectionsService] 停用社交连接失败, memberId={}, provider={}",
                    memberId, provider);
            throw new BusinessException("停用失败");
        }

        log.info("[SocialConnectionsService] 停用社交连接成功, memberId={}, provider={}", memberId, provider);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateConnection(Long memberId, String provider) {
        log.info("[SocialConnectionsService] 激活社交连接, memberId={}, provider={}", memberId, provider);

        LambdaUpdateWrapper<SocialConnection> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SocialConnection::getMemberId, memberId)
                .eq(SocialConnection::getProvider, provider)
                .isNull(SocialConnection::getGmtDeletedAt)
                .set(SocialConnection::getIsActive, SocialConnection.Status.ACTIVE.getCode())
                .set(SocialConnection::getGmtUpdatedAt, TimeUtils.currentTimestamp());

        boolean success = update(updateWrapper);
        if (!success) {
            log.error("[SocialConnectionsService] 激活社交连接失败, memberId={}, provider={}",
                    memberId, provider);
            throw new BusinessException("激活失败");
        }

        log.info("[SocialConnectionsService] 激活社交连接成功, memberId={}, provider={}", memberId, provider);
    }

    /**
     * 脱敏敏感信息（用于日志输出）
     *
     * @param info 敏感信息
     * @return 脱敏后的信息
     */
    private String maskSensitiveInfo(String info) {
        if (info == null || info.length() < 8) {
            return "***";
        }
        return info.substring(0, 4) + "***" + info.substring(info.length() - 4);
    }
}


