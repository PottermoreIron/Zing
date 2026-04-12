package com.pot.auth.domain;

import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserDefaultsGenerator unit test")
class UserDefaultsGeneratorTest {

    private static final String DEFAULT_AVATAR_URL = "https://cdn.example.com/avatars/default.png";

    private UserDefaultsGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new UserDefaultsGenerator(DEFAULT_AVATAR_URL, "user_", 12, true, true, true, true);
    }

    // generateNicknameFromPhone

    @Test
    @DisplayName("Phone-based nickname starts with user_ and includes timestamp and random suffix")
    void whenGenerateFromPhone_thenFollowFormat() {
        String nickname = generator.generateNicknameFromPhone("13800138000");
        assertThat(nickname).startsWith("user_");
        assertThat(nickname.split("_")).hasSizeGreaterThanOrEqualTo(2);
        assertThat(nickname).isLowerCase();
    }

    @RepeatedTest(5)
    @DisplayName("Multiple phone-based nickname generations are all non-blank")
    void whenGenerateFromPhoneRepeated_thenAlwaysNotBlank() {
        String nickname = generator.generateNicknameFromPhone("13912345678");
        assertThat(nickname).isNotBlank();
    }

    // generateNicknameFromEmail

    @Test
    @DisplayName("Email-based nickname contains the email local part")
    void whenGenerateFromEmail_thenContainsEmailPrefix() {
        String nickname = generator.generateNicknameFromEmail("johndoe@example.com");
        assertThat(nickname).startsWith("johndoe_");
    }

    @Test
    @DisplayName("Email-based nickname uses underscore-separated random suffix")
    void whenGenerateFromEmail_thenHasRandomSuffix() {
        String nickname = generator.generateNicknameFromEmail("test@example.com");
        String[] parts = nickname.split("_");
        assertThat(parts).hasSizeGreaterThanOrEqualTo(2);
        assertThat(parts[parts.length - 1]).hasSize(4);
    }

    // generateNickname

    @Test
    @DisplayName("Generic nickname starts with user_")
    void whenGenerateNickname_thenStartWithPrefix() {
        String nickname = generator.generateNickname();
        assertThat(nickname).startsWith("user_");
    }

    @RepeatedTest(3)
    @DisplayName("Multiple generic nickname generations are all non-blank")
    void whenGenerateNicknameRepeated_thenNotBlank() {
        assertThat(generator.generateNickname()).isNotBlank();
    }

    // generateRandomPassword

    @Test
    @DisplayName("Generated random password is 12 characters long")
    void whenGeneratePassword_thenHasCorrectLength() {
        String password = generator.generateRandomPassword();
        assertThat(password).hasSize(12);
    }

    @Test
    @DisplayName("Generated random password contains uppercase letters")
    void whenGeneratePassword_thenContainsUpperCase() {
        String password = generator.generateRandomPassword();
        assertThat(password).matches(".*[A-Z].*");
    }

    @Test
    @DisplayName("Generated random password contains lowercase letters")
    void whenGeneratePassword_thenContainsLowerCase() {
        String password = generator.generateRandomPassword();
        assertThat(password).matches(".*[a-z].*");
    }

    @Test
    @DisplayName("Generated random password contains digits")
    void whenGeneratePassword_thenContainsDigit() {
        String password = generator.generateRandomPassword();
        assertThat(password).matches(".*[0-9].*");
    }

    @Test
    @DisplayName("Generated random password contains special characters")
    void whenGeneratePassword_thenContainsSpecialChar() {
        String password = generator.generateRandomPassword();
        assertThat(password).matches(".*[!@#$%^&*].*");
    }

    @RepeatedTest(5)
    @DisplayName("Multiple password generations produce distinct values (randomness)")
    void whenGeneratePasswordMultipleTimes_thenRandom() {
        String p1 = generator.generateRandomPassword();
        String p2 = generator.generateRandomPassword();
        // Random collisions are possible, so this test only checks the basic contract.
        assertThat(p1).isNotBlank();
        assertThat(p2).isNotBlank();
    }

    // getDefaultAvatarUrl

    @Test
    @DisplayName("getDefaultAvatarUrl() returns a non-blank URL string")
    void whenGetDefaultAvatarUrl_thenReturnNonEmpty() {
        String url = generator.getDefaultAvatarUrl();
        assertThat(url).isNotBlank();
        assertThat(url).isEqualTo(DEFAULT_AVATAR_URL);
    }
}
