package com.pot.member.service.application.service;

import com.pot.member.facade.dto.RoleDTO;
import com.pot.member.service.application.assembler.PermissionAssembler;
import com.pot.member.service.application.dto.PermissionDTO;
import com.pot.member.service.application.query.GetMemberPermissionsQuery;
import com.pot.member.service.domain.model.member.Email;
import com.pot.member.service.domain.model.member.MemberAggregate;
import com.pot.member.service.domain.model.member.MemberId;
import com.pot.member.service.domain.model.member.MemberProfile;
import com.pot.member.service.domain.model.member.MemberStatus;
import com.pot.member.service.domain.model.member.Nickname;
import com.pot.member.service.domain.model.permission.PermissionAggregate;
import com.pot.member.service.domain.model.permission.PermissionId;
import com.pot.member.service.domain.model.role.RoleAggregate;
import com.pot.member.service.domain.model.role.RoleId;
import com.pot.member.service.domain.model.role.RoleName;
import com.pot.member.service.domain.repository.MemberRepository;
import com.pot.member.service.domain.repository.RoleRepository;
import com.pot.member.service.domain.service.PermissionDomainService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberPermissionApplicationService")
class MemberPermissionApplicationServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PermissionDomainService permissionDomainService;
    @Mock
    private PermissionAssembler permissionAssembler;

    @InjectMocks
    private MemberPermissionApplicationService service;

    private MemberAggregate persistedMember(Long id, Set<Long> roleIds) {
        return MemberAggregate.reconstitute(
                MemberId.of(id),
                Nickname.of("member" + id),
                Email.of("member" + id + "@test.com"),
                null,
                "$2a$10$hash",
                MemberStatus.ACTIVE,
                MemberProfile.empty(),
                roleIds,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null);
    }

    @Nested
    @DisplayName("getMemberPermissions()")
    class GetMemberPermissions {

        @Test
        @DisplayName("查询权限成功：调用领域服务并装配 DTO")
        void getMemberPermissions_returnsAssembledPermissions() {
            GetMemberPermissionsQuery query = GetMemberPermissionsQuery.builder().memberId(1L).build();
            PermissionAggregate permission = PermissionAggregate.reconstitute(
                    PermissionId.of(10L),
                    "member.read",
                    "查看会员",
                    "member",
                    "read",
                    "desc",
                    LocalDateTime.now(),
                    LocalDateTime.now());
            Set<PermissionDTO> expected = Set.of(PermissionDTO.builder().permissionCode("member.read").build());

            given(permissionDomainService.getMemberPermissions(1L)).willReturn(Set.of(permission));
            given(permissionAssembler.toDTOSet(any())).willReturn(expected);

            Set<PermissionDTO> result = service.getMemberPermissions(query);

            assertThat(result).isEqualTo(expected);
            then(permissionDomainService).should().getMemberPermissions(1L);
            then(permissionAssembler).should().toDTOSet(Set.of(permission));
        }
    }

    @Nested
    @DisplayName("getMemberRoles()")
    class GetMemberRoles {

        @Test
        @DisplayName("查询角色成功：返回 facade DTO 列表")
        void getMemberRoles_returnsFacadeRoles() {
            MemberAggregate member = persistedMember(1L, Set.of(20L));
            RoleAggregate role = RoleAggregate.reconstitute(
                    RoleId.of(20L),
                    RoleName.of("管理员"),
                    "ADMIN",
                    "系统管理员",
                    Set.of(100L),
                    LocalDateTime.now(),
                    LocalDateTime.now());

            given(memberRepository.findById(MemberId.of(1L))).willReturn(Optional.of(member));
            given(roleRepository.findByIds(Set.of(20L))).willReturn(List.of(role));

            List<RoleDTO> result = service.getMemberRoles(1L);

            assertThat(result)
                    .singleElement()
                    .satisfies(roleDTO -> {
                        assertThat(roleDTO.getRoleId()).isEqualTo(20L);
                        assertThat(roleDTO.getRoleCode()).isEqualTo("ADMIN");
                        assertThat(roleDTO.getRoleName()).isEqualTo("管理员");
                    });
        }
    }

    @Nested
    @DisplayName("getPermissionsBatch()")
    class GetPermissionsBatch {

        @Test
        @DisplayName("批量查询成功：直接复用领域层批量结果")
        void getPermissionsBatch_returnsDomainBatchResult() {
            List<Long> memberIds = List.of(1L, 2L);
            Map<Long, Set<String>> expected = Map.of(
                    1L, Set.of("member.read"),
                    2L, Set.of("member.write"));

            given(permissionDomainService.getMemberPermissionCodesBatch(memberIds)).willReturn(expected);

            Map<Long, Set<String>> result = service.getPermissionsBatch(memberIds);

            assertThat(result).isEqualTo(expected);
            then(permissionDomainService).should().getMemberPermissionCodesBatch(memberIds);
        }
    }
}