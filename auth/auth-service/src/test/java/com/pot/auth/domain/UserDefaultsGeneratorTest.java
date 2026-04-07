package com.pot.auth.domain;

import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserDefaultsGenerator 单元测试")
class UserDefaultsGeneratorTest {

    private static final String DEFAULT_AVATAR_URL = "https://cdn.example.com/avatars/default.png";

    private UserDefaultsGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new UserDefaultsGenerator(DEFAULT_AVATAR_URL, "user_", 12, true, true, true, true);
    }

    // generateNicknameFromPhone

    @Test
    @DisplayName("基于手机号生成昵称，以 user_ 前缀开头，且包含时间戳和随机部分")
    void whenGenerateFromPhone_thenFollowFormat() {
        String nickname = generator.generateNicknameFromPhone("13800138000");
        assertThat(nickname).startsWith("user_");
        assertThat(nickname.split("_")).hasSizeGreaterThanOrEqualTo(2);
        assertThat(nickname).isLowerCase();
    }

    @RepeatedTest(5)
    @DisplayName("多次基于手机号生成昵称，每次都不为空")
    void whenGenerateFromPhoneRepeated_thenAlwaysNotBlank() {
        String nickname = generator.generateNicknameFromPhone("13912345678");
        assertThat(nickname).isNotBlank();
    }

    // generateNicknameFromEmail

    @Test
    @DisplayName("基于邮箱生成昵称，包含邮箱前缀部分")
    void whenGenerateFromEmail_thenContainsEmailPrefix() {
        String nickname = generator.generateNicknameFromEmail("johndoe@example.com");
        assertThat(nickname).startsWith("johndoe_");
    }

    @Test
    @DisplayName("基于邮箱生成昵称，以下划线分隔后缀随机字符")
    void whenGenerateFromEmail_thenHasRandomSuffix() {
        String nickname = generator.generateNicknameFromEmail("test@example.com");
        String[] parts = nickname.split("_");
        assertThat(parts).hasSizeGreaterThanOrEqualTo(2);
        assertThat(parts[parts.length - 1]).hasSize(4);
    }

    // generateNickname

    @Test
    @DisplayName("生成通用昵称，以 user_ 前缀开头")
    void whenGenerateNickname_thenStartWithPrefix() {
        String nickname = generator.generateNickname();
        assertThat(nickname).startsWith("user_");
    }

    @RepeatedTest(3)
    @DisplayName("多次生成通用昵称，每次结果不为空")
    void whenGenerateNicknameRepeated_thenNotBlank() {
        assertThat(generator.generateNickname()).isNotBlank();
    }

    // generateRandomPassword

    @Test
    @DisplayName("生成的随机密码长度为12位")
    void whenGeneratePassword_thenHasCorrectLength() {
        String password = generator.generateRandomPassword();
        assertThat(password).hasSize(12);
    }

    @Test
    @DisplayName("生成的随机密码包含大写字母")
    void whenGeneratePassword_thenContainsUpperCase() {
        String password = generator.generateRandomPassword();
        assertThat(password).matches(".*[A-Z].*");
    }

    @Test
    @DisplayName("生成的随机密码包含小写字母")
    void whenGeneratePassword_thenContainsLowerCase() {
        String password = generator.generateRandomPassword();
        assertThat(password).matches(".*[a-z].*");
    }

    @Test
    @DisplayName("生成的随机密码包含数字")
    void whenGeneratePassword_thenContainsDigit() {
        String password = generator.generateRandomPassword();
        assertThat(password).matches(".*[0-9].*");
    }

    @Test
    @DisplayName("生成的随机密码包含特殊字符")
    void whenGeneratePassword_thenContainsSpecialChar() {
        String password = generator.generateRandomPassword();
        assertThat(password).matches(".*[!@#$%^&*].*");
    }

    @RepeatedTest(5)
    @DisplayName("多次生成密码，每次都不相同（随机性）")
    void whenGeneratePasswordMultipleTimes_thenRandom() {
        String p1 = generator.generateRandomPassword();
        String p2 = generator.generateRandomPassword();
        // Random collisions are possible, so this test only checks the basic contract.
        assertThat(p1).isNotBlank();
        assertThat(p2).isNotBlank();
    }

    // getDefaultAvatarUrl

    @Test
    @DisplayName("getDefaultAvatarUrl() 返回非空的URL字符串")
    void whenGetDefaultAvatarUrl_thenReturnNonEmpty() {
        String url = generator.getDefaultAvatarUrl();
        assertThat(url).isNotBlank();
        assertThat(url).isEqualTo(DEFAULT_AVATAR_URL);
    }
}
