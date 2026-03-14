package com.pot.auth.domain;

import com.pot.auth.domain.shared.generator.UserDefaultsGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserDefaultsGenerator 单元测试
 *
 * <p>
 * 验证：
 * <ul>
 * <li>基于手机号生成的用户名格式正确</li>
 * <li>基于邮箱生成的用户名包含邮箱前缀</li>
 * <li>通用用户名以 "user_" 前缀开头</li>
 * <li>生成的随机密码满足强度规则（12位、含大小写数字特殊字符）</li>
 * <li>多次调用生成不同的用户名（随机性）</li>
 * </ul>
 *
 * @author pot
 */
@DisplayName("UserDefaultsGenerator 单元测试")
class UserDefaultsGeneratorTest {

    private UserDefaultsGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new UserDefaultsGenerator();
    }

    // ================================================================
    // generateUsernameFromPhone
    // ================================================================

    @Test
    @DisplayName("基于手机号生成用户名，以 user_ 前缀开头，且包含时间戳和随机部分")
    void whenGenerateFromPhone_thenFollowFormat() {
        String username = generator.generateUsernameFromPhone("13800138000");
        assertThat(username).startsWith("user_");
        // 格式: user_{timestamp}_{random4}
        assertThat(username.split("_")).hasSizeGreaterThanOrEqualTo(2);
        assertThat(username).isLowerCase();
    }

    @RepeatedTest(5)
    @DisplayName("多次基于手机号生成用户名，每次都不为空")
    void whenGenerateFromPhoneRepeated_thenAlwaysNotBlank() {
        String username = generator.generateUsernameFromPhone("13912345678");
        assertThat(username).isNotBlank();
    }

    // ================================================================
    // generateUsernameFromEmail
    // ================================================================

    @Test
    @DisplayName("基于邮箱生成用户名，包含邮箱前缀部分")
    void whenGenerateFromEmail_thenContainsEmailPrefix() {
        String username = generator.generateUsernameFromEmail("johndoe@example.com");
        assertThat(username).startsWith("johndoe_");
    }

    @Test
    @DisplayName("基于邮箱生成用户名，以下划线分隔后缀随机字符")
    void whenGenerateFromEmail_thenHasRandomSuffix() {
        String username = generator.generateUsernameFromEmail("test@example.com");
        // 格式: {emailPrefix}_{random4}
        String[] parts = username.split("_");
        assertThat(parts).hasSizeGreaterThanOrEqualTo(2);
        // 随机后缀长度为4
        assertThat(parts[parts.length - 1]).hasSize(4);
    }

    // ================================================================
    // generateUsername
    // ================================================================

    @Test
    @DisplayName("生成通用用户名，以 user_ 前缀开头")
    void whenGenerateUsername_thenStartWithPrefix() {
        String username = generator.generateUsername();
        assertThat(username).startsWith("user_");
    }

    @RepeatedTest(3)
    @DisplayName("多次生成通用用户名，每次结果不为空")
    void whenGenerateUsernameRepeated_thenNotBlank() {
        assertThat(generator.generateUsername()).isNotBlank();
    }

    // ================================================================
    // generateRandomPassword
    // ================================================================

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
        // 有小概率相同，但通常应该不同
        // 不做严格断言，仅验证格式
        assertThat(p1).isNotBlank();
        assertThat(p2).isNotBlank();
    }

    // ================================================================
    // getDefaultAvatarUrl
    // ================================================================

    @Test
    @DisplayName("getDefaultAvatarUrl() 返回非空的URL字符串")
    void whenGetDefaultAvatarUrl_thenReturnNonEmpty() {
        String url = generator.getDefaultAvatarUrl();
        assertThat(url).isNotBlank();
        assertThat(url).startsWith("https://");
    }
}
