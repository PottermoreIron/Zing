package com.pot.member.service.domain.repository;

import com.pot.member.service.domain.model.social.SocialConnectionAggregate;

import java.util.List;
import java.util.Optional;

/**
 * 社交账号连接 Repository Port
 *
 * @author Pot
 * @since 2026-03-18
 */
public interface SocialConnectionRepository {

    SocialConnectionAggregate save(SocialConnectionAggregate connection);

    Optional<SocialConnectionAggregate> findActiveByMemberIdAndProvider(Long memberId, String provider);

    Optional<SocialConnectionAggregate> findActiveByProviderAndProviderId(String provider, String providerMemberId);

    Optional<SocialConnectionAggregate> findActiveByMemberIdAndWeChatOpenId(String weChatOpenId);

    List<SocialConnectionAggregate> findActiveByMemberId(Long memberId);

    long countActiveByMemberId(Long memberId);
}
