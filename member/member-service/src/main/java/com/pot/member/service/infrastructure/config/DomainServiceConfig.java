package com.pot.member.service.infrastructure.config;

import com.pot.member.service.domain.port.DomainEventPublisher;
import com.pot.member.service.domain.port.PasswordEncoder;
import com.pot.member.service.domain.repository.MemberRepository;
import com.pot.member.service.domain.repository.PermissionRepository;
import com.pot.member.service.domain.repository.RoleRepository;
import com.pot.member.service.domain.service.MemberDomainService;
import com.pot.member.service.domain.service.PermissionDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public MemberDomainService memberDomainService(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder) {
        return new MemberDomainService(memberRepository, passwordEncoder);
    }

    @Bean
    public PermissionDomainService permissionDomainService(
            MemberRepository memberRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            DomainEventPublisher eventPublisher) {
        return new PermissionDomainService(
                memberRepository,
                roleRepository,
                permissionRepository,
                eventPublisher);
    }
}