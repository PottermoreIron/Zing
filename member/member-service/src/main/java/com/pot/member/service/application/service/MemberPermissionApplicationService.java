package com.pot.member.service.application.service;

import com.pot.member.facade.dto.RoleDTO;
import com.pot.member.service.application.assembler.PermissionAssembler;
import com.pot.member.service.application.dto.PermissionDTO;
import com.pot.member.service.application.query.GetMemberPermissionsQuery;
import com.pot.member.service.domain.model.member.MemberAggregate;
import com.pot.member.service.domain.model.member.MemberId;
import com.pot.member.service.domain.model.permission.PermissionAggregate;
import com.pot.member.service.domain.model.role.RoleAggregate;
import com.pot.member.service.domain.repository.MemberRepository;
import com.pot.member.service.domain.repository.RoleRepository;
import com.pot.member.service.domain.service.PermissionDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberPermissionApplicationService {

    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final PermissionDomainService permissionDomainService;
    private final PermissionAssembler permissionAssembler;

    public Set<PermissionDTO> getMemberPermissions(GetMemberPermissionsQuery query) {
        Set<PermissionAggregate> permissions = permissionDomainService.getMemberPermissions(query.getMemberId());
        return permissionAssembler.toDTOSet(permissions);
    }

    public Set<String> getPermissionCodes(Long memberId) {
        return permissionDomainService.getMemberPermissions(memberId).stream()
                .map(PermissionAggregate::getPermissionCode)
                .collect(Collectors.toSet());
    }

    public List<RoleDTO> getMemberRoles(Long memberId) {
        MemberAggregate member = requireMember(memberId);
        return roleRepository.findByIds(member.getRoleIds()).stream()
                .map(this::toFacadeRoleDTO)
                .collect(Collectors.toList());
    }

    public Map<Long, Set<String>> getPermissionsBatch(List<Long> memberIds) {
        return permissionDomainService.getMemberPermissionCodesBatch(memberIds);
    }

    private MemberAggregate requireMember(Long memberId) {
        return memberRepository.findById(MemberId.of(memberId))
                .orElseThrow(() -> new IllegalArgumentException("会员不存在: " + memberId));
    }

    private RoleDTO toFacadeRoleDTO(RoleAggregate role) {
        return RoleDTO.builder()
                .roleId(role.getRoleId() != null ? role.getRoleId().value() : null)
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName() != null ? role.getRoleName().getValue() : null)
                .description(role.getDescription())
                .build();
    }
}