package com.pot.member.service.converter;

import com.pot.member.facade.dto.MemberDTO;
import com.pot.member.facade.dto.request.CreateMemberRequest;
import com.pot.member.service.entity.Member;
import com.pot.zing.framework.common.util.TimeUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Member实体转换器
 * <p>
 * 负责Member实体与DTO之间的转换，遵循单一职责原则
 * </p>
 *
 * @author Pot
 * @since 2025-10-20
 */
@Component
public class MemberConverter {

    /**
     * 将创建请求转换为Member实体
     *
     * @param request 创建请求
     * @return Member实体
     */
    public Member toEntity(CreateMemberRequest request) {
        if (request == null) {
            return null;
        }

        return Member.builder()
                .nickname(request.getNickname())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(request.getPassword())
                .status(Member.AccountStatus.ACTIVE.getCode())
                .gender(Member.Gender.UNKNOWN.getCode())
                .build();
    }

    /**
     * 将Member实体转换为DTO
     *
     * @param member Member实体
     * @return MemberDTO
     */
    public MemberDTO toDTO(Member member) {
        if (member == null) {
            return null;
        }

        MemberDTO.MemberDTOBuilder builder = MemberDTO.builder()
                .memberId(member.getMemberId())
                .username(member.getNickname())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .phone(member.getPhone())
                .avatarUrl(member.getAvatarUrl())
                .gender(member.getGender())
                .status(member.getStatus());

        // 转换时间为Unix Timestamp (毫秒)
        Optional.ofNullable(member.getGmtEmailVerifiedAt())
                .ifPresent(time -> builder.gmtEmailVerifiedAt(TimeUtils.toTimestamp(time)));

        Optional.ofNullable(member.getGmtPhoneVerifiedAt())
                .ifPresent(time -> builder.gmtPhoneVerifiedAt(TimeUtils.toTimestamp(time)));

        Optional.ofNullable(member.getGmtCreatedAt())
                .ifPresent(time -> builder.gmtCreatedAt(TimeUtils.toTimestamp(time)));

        return builder.build();
    }
}