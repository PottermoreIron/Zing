package com.pot.member.service.application.service;

import com.pot.member.service.application.assembler.RoleAssembler;
import com.pot.member.service.application.command.AssignRoleCommand;
import com.pot.member.service.application.dto.RoleDTO;
import com.pot.member.service.domain.model.role.RoleAggregate;
import com.pot.member.service.domain.model.role.RoleId;
import com.pot.member.service.domain.repository.RoleRepository;
import com.pot.member.service.domain.service.PermissionDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleApplicationService {

    private final RoleRepository roleRepository;
    private final PermissionDomainService permissionDomainService;
    private final RoleAssembler roleAssembler;

        public RoleDTO getRole(Long roleId) {
        RoleAggregate role = roleRepository.findById(RoleId.of(roleId))
                .orElse(null);
        return roleAssembler.toDTO(role);
    }

        public RoleDTO getRoleByCode(String roleCode) {
        RoleAggregate role = roleRepository.findByCode(roleCode)
                .orElse(null);
        return roleAssembler.toDTO(role);
    }

        public List<RoleDTO> getMemberRoles(Long memberId) {
        List<RoleAggregate> roles = roleRepository.findByMemberId(memberId);
        return roleAssembler.toDTOList(roles);
    }

        @Transactional
    public void assignRole(AssignRoleCommand command) {
        log.info("为会员分配角色: memberId={}, roleId={}", command.getMemberId(), command.getRoleId());

        permissionDomainService.assignRoleToMember(
                command.getMemberId(),
                command.getRoleId(),
                command.getOperator());
    }

        @Transactional
    public void revokeRole(Long memberId, Long roleId, String operator) {
        log.info("撤销会员角色: memberId={}, roleId={}", memberId, roleId);

        permissionDomainService.revokeRoleFromMember(memberId, roleId, operator);
    }

        public List<RoleDTO> getAllRoles() {
        List<RoleAggregate> roles = roleRepository.findAll();
        return roleAssembler.toDTOList(roles);
    }
}
