package com.pot.member.service.domain.repository;

import com.pot.member.service.domain.model.social.SocialConnectionAggregate;

import java.util.List;
import java.util.Optional;

public interface SocialConnectionRepository {

    SocialConnectionAggregate save(SocialConnectionAggregate connection);

    Optional<SocialConnectionAggregate> findActiveByMemberIdAndProvider(Long memberId, String provider);

    Optional<SocialConnectionAggregate> findActiveByProviderAndProviderId(String provider, String providerMemberId);

    Optional<SocialConnectionAggregate> findActiveByMemberIdAndWeChatOpenId(String weChatOpenId);

    List<SocialConnectionAggregate> findActiveByMemberId(Long memberId);

    long countActiveByMemberId(Long memberId);
}
