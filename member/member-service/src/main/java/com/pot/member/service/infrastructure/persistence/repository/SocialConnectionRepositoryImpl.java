package com.pot.member.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pot.member.service.domain.model.social.SocialConnection;
import com.pot.member.service.domain.repository.SocialConnectionRepository;
import com.pot.member.service.infrastructure.persistence.mapper.SocialConnectionsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 社交账号连接仓储实现
 *
 * @author Pot
 * @since 2026-03-18
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SocialConnectionRepositoryImpl implements SocialConnectionRepository {

    private final SocialConnectionsMapper mapper;

    @Override
    public SocialConnection save(SocialConnection domain) {
        com.pot.member.service.infrastructure.persistence.entity.SocialConnection entity = toEntity(domain);
        if (domain.getId() == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return toDomain(entity);
    }

    @Override
    public Optional<SocialConnection> findActiveByMemberIdAndProvider(Long memberId, String provider) {
        LambdaQueryWrapper<com.pot.member.service.infrastructure.persistence.entity.SocialConnection> q = new LambdaQueryWrapper<>();
        q.eq(com.pot.member.service.infrastructure.persistence.entity.SocialConnection::getMemberId, memberId)
                .eq(com.pot.member.service.infrastructure.persistence.entity.SocialConnection::getProvider, provider.toLowerCase())
                .isNull(com.pot.member.service.infrastructure.persistence.entity.SocialConnection::getGmtDeletedAt)
                .last("LIMIT 1");
        var entity = mapper.selectOne(q);
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public Optional<SocialConnection> findActiveByProviderAndProviderId(String provider, String providerMemberId) {
        LambdaQueryWrapper<com.pot.member.service.infrastructure.persistence.entity.SocialConnection> q = new LambdaQueryWrapper<>();
        q.eq(com.pot.member.service.infrastructure.persistence.entity.SocialConnection::getProvider, provider.toLowerCase())
                .eq(com.pot.member.service.infrastructure.persistence.entity.SocialConnection::getProviderMemberId, providerMemberId)
                .isNull(com.pot.member.service.infrastructure.persistence.entity.SocialConnection::getGmtDeletedAt)
                .last("LIMIT 1");
        var entity = mapper.selectOne(q);
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public Optional<SocialConnection> findActiveByMemberIdAndWeChatOpenId(String weChatOpenId) {
        // WeChat is stored as provider = "wechat"
        LambdaQueryWrapper<com.pot.member.service.infrastructure.persistence.entity.SocialConnection> q = new LambdaQueryWrapper<>();
        q.eq(com.pot.member.service.infrastructure.persistence.entity.SocialConnection::getProvider, "wechat")
                .eq(com.pot.member.service.infrastructure.persistence.entity.SocialConnection::getProviderMemberId, weChatOpenId)
                .isNull(com.pot.member.service.infrastructure.persistence.entity.SocialConnection::getGmtDeletedAt)
                .last("LIMIT 1");
        var entity = mapper.selectOne(q);
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public List<SocialConnection> findActiveByMemberId(Long memberId) {
        LambdaQueryWrapper<com.pot.member.service.infrastructure.persistence.entity.SocialConnection> q = new LambdaQueryWrapper<>();
        q.eq(com.pot.member.service.infrastructure.persistence.entity.SocialConnection::getMemberId, memberId)
                .isNull(com.pot.member.service.infrastructure.persistence.entity.SocialConnection::getGmtDeletedAt)
                .orderByDesc(com.pot.member.service.infrastructure.persistence.entity.SocialConnection::getGmtCreatedAt);
        return mapper.selectList(q).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countActiveByMemberId(Long memberId) {
        LambdaQueryWrapper<com.pot.member.service.infrastructure.persistence.entity.SocialConnection> q = new LambdaQueryWrapper<>();
        q.eq(com.pot.member.service.infrastructure.persistence.entity.SocialConnection::getMemberId, memberId)
                .isNull(com.pot.member.service.infrastructure.persistence.entity.SocialConnection::getGmtDeletedAt)
                .eq(com.pot.member.service.infrastructure.persistence.entity.SocialConnection::getIsActive, 1);
        return mapper.selectCount(q);
    }

    private SocialConnection toDomain(com.pot.member.service.infrastructure.persistence.entity.SocialConnection e) {
        return SocialConnection.reconstitute(
                e.getId(), e.getMemberId(), e.getProvider(),
                e.getProviderMemberId(), e.getProviderUsername(),
                e.getProviderEmail(), e.getAccessToken(), e.getRefreshToken(),
                e.getGmtTokenExpiresAt(), e.getScope(), e.getExtendJson(),
                e.getIsActive() != null && e.getIsActive() == 1,
                e.getGmtCreatedAt() != null ? LocalDateTime.ofEpochSecond(e.getGmtCreatedAt() / 1000, 0, ZoneOffset.UTC)
                        : null,
                e.getGmtUpdatedAt() != null ? LocalDateTime.ofEpochSecond(e.getGmtUpdatedAt() / 1000, 0, ZoneOffset.UTC)
                        : null,
                e.getGmtDeletedAt() != null ? LocalDateTime.ofEpochSecond(e.getGmtDeletedAt() / 1000, 0, ZoneOffset.UTC)
                        : null);
    }

    private com.pot.member.service.infrastructure.persistence.entity.SocialConnection toEntity(SocialConnection d) {
        var e = new com.pot.member.service.infrastructure.persistence.entity.SocialConnection();
        if (d.getId() != null)
            e.setId(d.getId());
        e.setMemberId(d.getMemberId());
        if (d.getProvider() != null) {
            e.setProvider(com.pot.member.service.infrastructure.persistence.entity.SocialConnection.Provider.fromCode(d.getProvider()));
        }
        e.setProviderMemberId(d.getProviderMemberId());
        e.setProviderUsername(d.getProviderUsername());
        e.setProviderEmail(d.getProviderEmail());
        e.setAccessToken(d.getAccessToken());
        e.setRefreshToken(d.getRefreshToken());
        e.setGmtTokenExpiresAt(d.getTokenExpiresAt());
        e.setScope(d.getScope());
        e.setExtendJson(d.getExtendJson());
        e.setIsActive(d.isActive() ? 1 : 0);
        long nowMs = System.currentTimeMillis();
        if (d.getCreatedAt() != null) {
            e.setGmtCreatedAt(d.getCreatedAt().toEpochSecond(ZoneOffset.UTC) * 1000);
        } else {
            e.setGmtCreatedAt(nowMs);
        }
        e.setGmtUpdatedAt(nowMs);
        if (d.getDeletedAt() != null) {
            e.setGmtDeletedAt(d.getDeletedAt().toEpochSecond(ZoneOffset.UTC) * 1000);
        }
        return e;
    }
}
