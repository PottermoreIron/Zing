package com.pot.member.service.domain.model.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemberProfile")
class MemberProfileTest {

    @Nested
    @DisplayName("empty()")
    class Empty {

        @Test
        @DisplayName("All fields are null")
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
        @DisplayName("Builder sets all fields")
        void builder_allFields() {
            MemberProfile profile = MemberProfile.builder()
                    .nickname("TestNickname")
                    .firstName("San")
                    .lastName("Zhang")
                    .gender(1)
                    .birthDate("1990-01-01")
                    .bio("A brief bio")
                    .countryCode("CN")
                    .region("Shanghai")
                    .city("Shanghai")
                    .timezone("Asia/Shanghai")
                    .locale("zh-CN")
                    .build();

            assertThat(profile.getNickname()).isEqualTo("TestNickname");
            assertThat(profile.getFirstName()).isEqualTo("San");
            assertThat(profile.getLastName()).isEqualTo("Zhang");
            assertThat(profile.getGender()).isEqualTo(1);
            assertThat(profile.getBirthDate()).isEqualTo("1990-01-01");
            assertThat(profile.getBio()).isEqualTo("A brief bio");
            assertThat(profile.getCountryCode()).isEqualTo("CN");
            assertThat(profile.getRegion()).isEqualTo("Shanghai");
            assertThat(profile.getCity()).isEqualTo("Shanghai");
            assertThat(profile.getTimezone()).isEqualTo("Asia/Shanghai");
            assertThat(profile.getLocale()).isEqualTo("zh-CN");
        }

        @Test
        @DisplayName("Builder with partial fields, others are null")
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
        @DisplayName("Returns new instance and does not modify original (immutable)")
        void withNickname_immutable() {
            MemberProfile original = MemberProfile.builder()
                    .nickname("OriginalNickname")
                    .city("Beijing")
                    .build();

            MemberProfile updated = original.withNickname("NewNickname");

            assertThat(updated.getNickname()).isEqualTo("NewNickname");
            assertThat(updated.getCity()).isEqualTo("Beijing");
            assertThat(original.getNickname()).isEqualTo("OriginalNickname");
        }

        @Test
        @DisplayName("withNickname(null) returns new instance with null nickname")
        void withNickname_null() {
            MemberProfile profile = MemberProfile.builder().nickname("TestNickname").build();
            MemberProfile updated = profile.withNickname(null);
            assertThat(updated.getNickname()).isNull();
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("MemberProfile is a final class and cannot be extended")
        void isFinalClass() {
            assertThat(MemberProfile.class).isFinal();
        }
    }
}
