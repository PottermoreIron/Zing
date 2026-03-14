package com.pot.auth.domain;

import com.pot.auth.domain.authorization.valueobject.PermissionDigest;
import com.pot.auth.domain.shared.exception.InvalidEmailException;
import com.pot.auth.domain.shared.exception.InvalidIpAddressException;
import com.pot.auth.domain.shared.exception.InvalidPhoneException;
import com.pot.auth.domain.shared.exception.WeakPasswordException;
import com.pot.auth.domain.shared.valueobject.Email;
import com.pot.auth.domain.shared.valueobject.IpAddress;
import com.pot.auth.domain.shared.valueobject.Password;
import com.pot.auth.domain.shared.valueobject.Phone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 领域值对象单元测试
 *
 * <p>
 * 覆盖：Email、Phone、Password、IpAddress、PermissionDigest
 *
 * @author pot
 */
@DisplayName("领域值对象单元测试")
class DomainValueObjectsTest {

    // ================================================================
    // Email
    // ================================================================

    @Nested
    @DisplayName("Email 值对象")
    class EmailTests {

        @ParameterizedTest(name = "合法邮箱: {0}")
        @ValueSource(strings = {
                "test@example.com",
                "user.name+tag@subdomain.example.org",
                "admin@corp.cn",
                "TEST@GMAIL.COM",
        })
        @DisplayName("合法邮箱地址，创建成功且规范化为小写")
        void whenValidEmail_thenCreateSuccessfully(String email) {
            Email e = Email.of(email);
            assertThat(e.value()).isEqualTo(email.toLowerCase());
        }

        @ParameterizedTest(name = "非法邮箱: {0}")
        @ValueSource(strings = {
                "not-an-email",
                "@example.com",
                "user@",
                "user @example.com",
                "",
        })
        @DisplayName("非法邮箱格式，抛出 InvalidEmailException")
        void whenInvalidEmail_thenThrowException(String email) {
            assertThatThrownBy(() -> Email.of(email))
                    .isInstanceOf(InvalidEmailException.class);
        }

        @Test
        @DisplayName("null 邮箱，抛出 InvalidEmailException")
        void whenNullEmail_thenThrowException() {
            assertThatThrownBy(() -> Email.of(null))
                    .isInstanceOf(InvalidEmailException.class);
        }

        @Test
        @DisplayName("getDomain() 返回正确的域名")
        void whenGetDomain_thenReturnDomainPart() {
            Email email = Email.of("user@gmail.com");
            assertThat(email.getDomain()).isEqualTo("gmail.com");
        }

        @Test
        @DisplayName("getLocalPart() 返回正确的用户名部分")
        void whenGetLocalPart_thenReturnLocalPart() {
            Email email = Email.of("user@gmail.com");
            assertThat(email.getLocalPart()).isEqualTo("user");
        }

        @Test
        @DisplayName("isCorporateEmail() 对企业邮箱返回true")
        void whenCorporateEmail_thenReturnTrue() {
            assertThat(Email.of("admin@acme.io").isCorporateEmail()).isTrue();
        }

        @Test
        @DisplayName("isCorporateEmail() 对个人邮箱(gmail)返回false")
        void whenPersonalGmail_thenReturnFalse() {
            assertThat(Email.of("user@gmail.com").isCorporateEmail()).isFalse();
        }
    }

    // ================================================================
    // Phone
    // ================================================================

    @Nested
    @DisplayName("Phone 值对象")
    class PhoneTests {

        @ParameterizedTest(name = "合法手机号: {0}")
        @ValueSource(strings = {
                "13800138000",
                "18612345678",
                "+8613800138000",
                "+14155552671",
        })
        @DisplayName("合法手机号，创建成功")
        void whenValidPhone_thenCreateSuccessfully(String phone) {
            assertThat(Phone.of(phone)).isNotNull();
        }

        @ParameterizedTest(name = "非法手机号: {0}")
        @ValueSource(strings = {
                "+0123456789",
                "abcdefghijk",
                "00000000000",
        })
        @DisplayName("非法手机号格式，抛出 InvalidPhoneException")
        void whenInvalidPhone_thenThrowException(String phone) {
            assertThatThrownBy(() -> Phone.of(phone))
                    .isInstanceOf(InvalidPhoneException.class);
        }

        @Test
        @DisplayName("null 手机号，抛出 InvalidPhoneException")
        void whenNullPhone_thenThrowException() {
            assertThatThrownBy(() -> Phone.of(null))
                    .isInstanceOf(InvalidPhoneException.class);
        }

        @Test
        @DisplayName("中国大陆手机号 isChinaMobile() 返回 true")
        void whenChinaMobile_thenReturnTrue() {
            assertThat(Phone.of("13800138000").isChinaMobile()).isTrue();
        }

        @Test
        @DisplayName("国际手机号 isChinaMobile() 返回 false")
        void whenInternational_thenReturnFalse() {
            assertThat(Phone.of("+14155552671").isChinaMobile()).isFalse();
        }

        @Test
        @DisplayName("toInternationalFormat() 为中国大陆号码加 +86 前缀")
        void whenChinaMobileToInternational_thenAddPrefix() {
            assertThat(Phone.of("13800138000").toInternationalFormat())
                    .isEqualTo("+8613800138000");
        }

        @Test
        @DisplayName("toMasked() 中间4位被掩码")
        void whenToMasked_thenHideMiddleDigits() {
            String masked = Phone.of("13800138000").toMasked();
            assertThat(masked).contains("****");
            assertThat(masked).startsWith("138");
            assertThat(masked).endsWith("8000");
        }
    }

    // ================================================================
    // Password
    // ================================================================

    @Nested
    @DisplayName("Password 值对象")
    class PasswordTests {

        @Test
        @DisplayName("强密码（含大写、小写、数字、特殊字符），创建成功")
        void whenStrongPassword_thenCreateSuccessfully() {
            assertThat(Password.of("Password123!")).isNotNull();
        }

        @Test
        @DisplayName("密码长度不足8位，抛出 WeakPasswordException")
        void whenTooShort_thenThrowException() {
            assertThatThrownBy(() -> Password.of("Aa1!"))
                    .isInstanceOf(WeakPasswordException.class)
                    .hasMessageContaining("8");
        }

        @Test
        @DisplayName("密码超过64位，抛出 WeakPasswordException")
        void whenTooLong_thenThrowException() {
            String longPwd = "Aa1!" + "a".repeat(65);
            assertThatThrownBy(() -> Password.of(longPwd))
                    .isInstanceOf(WeakPasswordException.class);
        }

        @Test
        @DisplayName("无大写字母，抛出 WeakPasswordException")
        void whenNoUppercase_thenThrowException() {
            assertThatThrownBy(() -> Password.of("password123!"))
                    .isInstanceOf(WeakPasswordException.class)
                    .hasMessageContaining("大写");
        }

        @Test
        @DisplayName("无小写字母，抛出 WeakPasswordException")
        void whenNoLowercase_thenThrowException() {
            assertThatThrownBy(() -> Password.of("PASSWORD123!"))
                    .isInstanceOf(WeakPasswordException.class)
                    .hasMessageContaining("小写");
        }

        @Test
        @DisplayName("无数字，抛出 WeakPasswordException")
        void whenNoDigit_thenThrowException() {
            assertThatThrownBy(() -> Password.of("PasswordABC!"))
                    .isInstanceOf(WeakPasswordException.class)
                    .hasMessageContaining("数字");
        }

        @Test
        @DisplayName("null 密码，抛出 WeakPasswordException")
        void whenNull_thenThrowException() {
            assertThatThrownBy(() -> Password.of(null))
                    .isInstanceOf(WeakPasswordException.class);
        }

        @Test
        @DisplayName("calculateStrength() 包含特殊字符时强度 > 60")
        void whenSpecialCharsIncluded_thenHighStrength() {
            int strength = Password.of("Password123!").calculateStrength();
            assertThat(strength).isGreaterThan(60);
        }

        @Test
        @DisplayName("calculateStrength() 仅满足最低要求时强度在合理范围内")
        void whenMinimalPassword_thenReasonableStrength() {
            int strength = Password.of("Password1").calculateStrength();
            assertThat(strength).isBetween(0, 100);
        }
    }

    // ================================================================
    // IpAddress
    // ================================================================

    @Nested
    @DisplayName("IpAddress 值对象")
    class IpAddressTests {

        @ParameterizedTest(name = "合法IP: {0}")
        @ValueSource(strings = {
                "127.0.0.1",
                "192.168.1.100",
                "10.0.0.1",
                "255.255.255.255",
                "0.0.0.0",
        })
        @DisplayName("合法IPv4地址，创建成功")
        void whenValidIpv4_thenCreateSuccessfully(String ip) {
            assertThat(IpAddress.of(ip)).isNotNull();
        }

        @Test
        @DisplayName("null IP，抛出异常")
        void whenNull_thenThrowException() {
            assertThatThrownBy(() -> IpAddress.of(null))
                    .isInstanceOf(InvalidIpAddressException.class);
        }

        @Test
        @DisplayName("空字符串IP，抛出异常")
        void whenEmpty_thenThrowException() {
            assertThatThrownBy(() -> IpAddress.of(""))
                    .isInstanceOf(InvalidIpAddressException.class);
        }
    }

    // ================================================================
    // PermissionDigest
    // ================================================================

    @Nested
    @DisplayName("PermissionDigest 值对象")
    class PermissionDigestTests {

        @Test
        @DisplayName("从相同权限集合计算的摘要应相等（排序无关）")
        void whenSamePermissions_thenSameDigest() {
            Set<String> perms1 = Set.of("b", "a", "c");
            Set<String> perms2 = Set.of("c", "b", "a");
            PermissionDigest d1 = PermissionDigest.from(perms1);
            PermissionDigest d2 = PermissionDigest.from(perms2);
            assertThat(d1).isEqualTo(d2);
        }

        @Test
        @DisplayName("从不同权限集合计算的摘要不相等")
        void whenDifferentPermissions_thenDifferentDigest() {
            PermissionDigest d1 = PermissionDigest.from(Set.of("user:read"));
            PermissionDigest d2 = PermissionDigest.from(Set.of("admin:write"));
            assertThat(d1).isNotEqualTo(d2);
        }

        @Test
        @DisplayName("空权限集合，返回 empty() 摘要")
        void whenEmptyPermissions_thenReturnEmptyDigest() {
            PermissionDigest digest = PermissionDigest.from(Set.of());
            assertThat(digest).isEqualTo(PermissionDigest.empty());
        }

        @Test
        @DisplayName("matches() 权限集合匹配时返回 true")
        void whenMatchingPermissions_thenReturnTrue() {
            Set<String> perms = Set.of("user:read", "user:write");
            PermissionDigest digest = PermissionDigest.from(perms);
            assertThat(digest.matches(perms)).isTrue();
        }

        @Test
        @DisplayName("matches() 权限集合不匹配时返回 false")
        void whenNotMatchingPermissions_thenReturnFalse() {
            PermissionDigest digest = PermissionDigest.from(Set.of("user:read"));
            assertThat(digest.matches(Set.of("admin:write"))).isFalse();
        }

        @Test
        @DisplayName("shortValue() 返回前8位摘要")
        void whenShortValue_thenReturnFirst8Chars() {
            PermissionDigest digest = PermissionDigest.from(Set.of("user:read"));
            assertThat(digest.shortValue()).hasSize(8);
            assertThat(digest.value()).startsWith(digest.shortValue());
        }

        @Test
        @DisplayName("PermissionDigest 构造时，非MD5格式字符串抛出 IllegalArgumentException")
        void whenInvalidFormat_thenThrowException() {
            assertThatThrownBy(() -> new PermissionDigest("not-a-md5"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("PermissionDigest 构造时，空字符串抛出 IllegalArgumentException")
        void whenBlankValue_thenThrowException() {
            assertThatThrownBy(() -> new PermissionDigest(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
