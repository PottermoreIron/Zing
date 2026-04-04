package com.pot.member.service.domain.model.member;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 会员个人资料值对象
 *
 * <p>
 * 将 profile 信息从核心身份信息中拆离，符合"单一职责"：
 * <ul>
 * <li>{@link MemberAggregate} 关注认证/鉴权核心属性</li>
 * <li>{@link MemberProfile} 关注可变的展示/偏好信息</li>
 * </ul>
 *
 * @author Pot
 * @since 2026-03-18
 */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class MemberProfile {

    private final String nickname;
    private final String firstName;
    private final String lastName;
    private final Integer gender;
    private final String birthDate;
    private final String bio;
    private final String countryCode;
    private final String region;
    private final String city;
    private final String timezone;
    private final String locale;

    /**
     * 创建空 profile（用于新注册用户）
     */
    public static MemberProfile empty() {
        return builder().build();
    }

    /**
     * 返回一个修改了 nickname 的新实例（值对象不可变）
     */
    public MemberProfile withNickname(String nickname) {
        return toBuilder()
                .nickname(nickname)
                .build();
    }
}
