package com.pot.member.service.domain.model.member;

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

    private MemberProfile(Builder builder) {
        this.nickname = builder.nickname;
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.gender = builder.gender;
        this.birthDate = builder.birthDate;
        this.bio = builder.bio;
        this.countryCode = builder.countryCode;
        this.region = builder.region;
        this.city = builder.city;
        this.timezone = builder.timezone;
        this.locale = builder.locale;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 创建空 profile（用于新注册用户）
     */
    public static MemberProfile empty() {
        return new Builder().build();
    }

    /**
     * 返回一个修改了 nickname 的新实例（值对象不可变）
     */
    public MemberProfile withNickname(String nickname) {
        return builder()
                .nickname(nickname)
                .firstName(this.firstName)
                .lastName(this.lastName)
                .gender(this.gender)
                .birthDate(this.birthDate)
                .bio(this.bio)
                .countryCode(this.countryCode)
                .region(this.region)
                .city(this.city)
                .timezone(this.timezone)
                .locale(this.locale)
                .build();
    }

    public static final class Builder {
        private String nickname;
        private String firstName;
        private String lastName;
        private Integer gender;
        private String birthDate;
        private String bio;
        private String countryCode;
        private String region;
        private String city;
        private String timezone;
        private String locale;

        public Builder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder gender(Integer gender) {
            this.gender = gender;
            return this;
        }

        public Builder birthDate(String birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        public Builder bio(String bio) {
            this.bio = bio;
            return this;
        }

        public Builder countryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }

        public Builder locale(String locale) {
            this.locale = locale;
            return this;
        }

        public MemberProfile build() {
            return new MemberProfile(this);
        }
    }
}
