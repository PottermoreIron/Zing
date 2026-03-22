package com.pot.member.service.domain.model.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MemberProfile} 单元测试
 */
@DisplayName("MemberProfile")
class MemberProfileTest {

    @Nested
    @DisplayName("empty()")
    class Empty {

        @Test
        @DisplayName("所有字段均为 null")
        void empty_allFieldsNull() {
            MemberProfile profile = MemberProfile.empty();

            assertThat(profile.getNickname()).isNull();
            assertThat(profile.getFirstName()).isNull();
            assertThat(profile.getLastName()).isNull();
            assertThat(profile.getGender()).isNull();
            assertThat(profile.getBirthDate()).isNull();
            assertThat(profile.getBio()).isNull();
            assertThat(profile.getCountryCode()).isNull();
            assertThat(profile.getRegion()).isNull();
            assertThat(profile.getCity()).isNull();
            assertThat(profile.getTimezone()).isNull();
            assertThat(profile.getLocale()).isNull();
        }
    }

    @Nested
    @DisplayName("builder()")
    class Builder {

        @Test
        @DisplayName("builder 设置全部字段")
        void builder_allFields() {
            MemberProfile profile = MemberProfile.builder()
                    .nickname("昵称")
                    .firstName("三")
                    .lastName("张")
                    .gender(1)
                    .birthDate("1990-01-01")
                    .bio("简介")
                    .countryCode("CN")
                    .region("上海市")
                    .city("上海")
                    .timezone("Asia/Shanghai")
                    .locale("zh-CN")
                    .build();

            assertThat(profile.getNickname()).isEqualTo("昵称");
            assertThat(profile.getFirstName()).isEqualTo("三");
            assertThat(profile.getLastName()).isEqualTo("张");
            assertThat(profile.getGender()).isEqualTo(1);
            assertThat(profile.getBirthDate()).isEqualTo("1990-01-01");
            assertThat(profile.getBio()).isEqualTo("简介");
            assertThat(profile.getCountryCode()).isEqualTo("CN");
            assertThat(profile.getRegion()).isEqualTo("上海市");
            assertThat(profile.getCity()).isEqualTo("上海");
            assertThat(profile.getTimezone()).isEqualTo("Asia/Shanghai");
            assertThat(profile.getLocale()).isEqualTo("zh-CN");
        }

        @Test
        @DisplayName("builder 只设置部分字段，其余为 null")
        void builder_partialFields() {
            MemberProfile profile = MemberProfile.builder()
                    .nickname("test")
                    .build();

            assertThat(profile.getNickname()).isEqualTo("test");
            assertThat(profile.getCity()).isNull();
        }
    }

    @Nested
    @DisplayName("withNickname()")
    class WithNickname {

        @Test
        @DisplayName("返回新实例，不修改原实例（不可变）")
        void withNickname_immutable() {
            MemberProfile original = MemberProfile.builder()
                    .nickname("原昵称")
                    .city("北京")
                    .build();

            MemberProfile updated = original.withNickname("新昵称");

            // 新实例应有新昵称
            assertThat(updated.getNickname()).isEqualTo("新昵称");
            // 其余字段应从原实例复制
            assertThat(updated.getCity()).isEqualTo("北京");
            // 原实例不变
            assertThat(original.getNickname()).isEqualTo("原昵称");
        }

        @Test
        @DisplayName("withNickname(null) 返回 nickname 为 null 的新实例")
        void withNickname_null() {
            MemberProfile profile = MemberProfile.builder().nickname("昵称").build();
            MemberProfile updated = profile.withNickname(null);
            assertThat(updated.getNickname()).isNull();
        }
    }

    @Nested
    @DisplayName("不可变性")
    class Immutability {

        @Test
        @DisplayName("MemberProfile 是 final 类，不可被继承")
        void isFinalClass() {
            assertThat(MemberProfile.class).isFinal();
        }
    }
}
