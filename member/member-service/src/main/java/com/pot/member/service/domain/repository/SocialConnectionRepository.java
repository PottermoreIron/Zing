package com.pot.member.service.domain.repository;

import com.pot.member.service.domain.model.social.SocialConnection;

import java.util.List;
import java.util.Optional;

/**
 * 社交账号连接 Repository Port
 *
 * @author Pot
 * @since 2026-03-18
 */
public interface SocialConnectionRepository {

    SocialConnection save(SocialConnection connection);

    Optional<SocialConnection> findActiveByMemberIdAndProvider(Long memberId, String provider);

    Optional<SocialConnection> findActiveByProviderAndProviderId(String provider, String providerMemberId);

    Optional<SocialConnection> findActiveByMemberIdAndWeChatOpenId(String weChatOpenId);

    List<SocialConnection> findActiveByMemberId(Long memberId);

    long countActiveByMemberId(Long memberId);
}
