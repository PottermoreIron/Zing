package com.pot.member.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pot.member.service.domain.model.social.SocialConnectionAggregate;
import com.pot.member.service.domain.repository.SocialConnectionRepository;
import com.pot.member.service.infrastructure.persistence.entity.SocialConnection;
import com.pot.member.service.infrastructure.persistence.mapper.SocialConnectionsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SocialConnectionRepositoryImpl implements SocialConnectionRepository {

    private final SocialConnectionsMapper mapper;

    @Override
    public SocialConnectionAggregate save(SocialConnectionAggregate domain) {
        SocialConnection entity = toEntity(domain);
        if (domain.getId() == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return toDomain(entity);
    }

    @Override
    public Optional<SocialConnectionAggregate> findActiveByMemberIdAndProvider(Long memberId, String provider) {
        LambdaQueryWrapper<SocialConnection> q = new LambdaQueryWrapper<>();
        q.eq(SocialConnection::getMemberId, memberId)
                .eq(SocialConnection::getProvider, provider.toLowerCase())
                .isNull(SocialConnection::getGmtDeletedAt)
                .last("LIMIT 1");
        var entity = mapper.selectOne(q);
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public Optional<SocialConnectionAggregate> findActiveByProviderAndProviderId(String provider,
                                                                                 String providerMemberId) {
        LambdaQueryWrapper<SocialConnection> q = new LambdaQueryWrapper<>();
        q.eq(SocialConnection::getProvider, provider.toLowerCase())
                .eq(SocialConnection::getProviderMemberId, providerMemberId)
                .isNull(SocialConnection::getGmtDeletedAt)
                .last("LIMIT 1");
        var entity = mapper.selectOne(q);
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public Optional<SocialConnectionAggregate> findActiveByMemberIdAndWeChatOpenId(String weChatOpenId) {
        // WeChat is stored as provider = "wechat"
        LambdaQueryWrapper<SocialConnection> q = new LambdaQueryWrapper<>();
        q.eq(SocialConnection::getProvider, "wechat")
                .eq(SocialConnection::getProviderMemberId, weChatOpenId)
                .isNull(SocialConnection::getGmtDeletedAt)
                .last("LIMIT 1");
        var entity = mapper.selectOne(q);
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public List<SocialConnectionAggregate> findActiveByMemberId(Long memberId) {
        LambdaQueryWrapper<SocialConnection> q = new LambdaQueryWrapper<>();
        q.eq(SocialConnection::getMemberId, memberId)
                .isNull(SocialConnection::getGmtDeletedAt)
                .orderByDesc(SocialConnection::getGmtCreatedAt);
        return mapper.selectList(q).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countActiveByMemberId(Long memberId) {
        LambdaQueryWrapper<SocialConnection> q = new LambdaQueryWrapper<>();
        q.eq(SocialConnection::getMemberId, memberId)
                .isNull(SocialConnection::getGmtDeletedAt)
                .eq(SocialConnection::getIsActive, 1);
        return mapper.selectCount(q);
    }

    private SocialConnectionAggregate toDomain(SocialConnection e) {
        return SocialConnectionAggregate.reconstitute(
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

    private SocialConnection toEntity(SocialConnectionAggregate d) {
        var e = new SocialConnection();
        if (d.getId() != null)
            e.setId(d.getId());
        e.setMemberId(d.getMemberId());
        if (d.getProvider() != null) {
            e.setProvider(SocialConnection.Provider.fromCode(d.getProvider()));
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
