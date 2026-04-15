package com.pot.auth.domain;

import com.pot.auth.domain.authorization.valueobject.PermissionDigest;
import com.pot.auth.domain.shared.exception.DomainException;
import com.pot.auth.domain.shared.exception.InvalidIpAddressException;
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

@DisplayName("Domain value object unit tests")
class DomainValueObjectsTest {

    // Email

    @Nested
    @DisplayName("Email value object")
    class EmailTests {

        @ParameterizedTest(name = "Valid email: {0}")
        @ValueSource(strings = {
                "test@example.com",
                "user.name+tag@subdomain.example.org",
                "admin@corp.cn",
                "TEST@GMAIL.COM",
        })
        @DisplayName("Valid email address creates successfully and normalizes to lowercase")
        void whenValidEmail_thenCreateSuccessfully(String email) {
            Email e = Email.of(email);
            assertThat(e.value()).isEqualTo(email.toLowerCase());
        }

        @ParameterizedTest(name = "Invalid email: {0}")
        @ValueSource(strings = {
                "not-an-email",
                "@example.com",
                "user@",
                "user @example.com",
                "",
        })
        @DisplayName("Invalid email format throws DomainException")
        void whenInvalidEmail_thenThrowException(String email) {
            assertThatThrownBy(() -> Email.of(email))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("Null email throws DomainException")
        void whenNullEmail_thenThrowException() {
            assertThatThrownBy(() -> Email.of(null))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("getDomain() returns the correct domain name")
        void whenGetDomain_thenReturnDomainPart() {
            Email email = Email.of("user@gmail.com");
            assertThat(email.getDomain()).isEqualTo("gmail.com");
        }

        @Test
        @DisplayName("getLocalPart() returns the correct local part")
        void whenGetLocalPart_thenReturnLocalPart() {
            Email email = Email.of("user@gmail.com");
            assertThat(email.getLocalPart()).isEqualTo("user");
        }

        @Test
        @DisplayName("isCorporateEmail() returns true for corporate email")
        void whenCorporateEmail_thenReturnTrue() {
            assertThat(Email.of("admin@acme.io").isCorporateEmail()).isTrue();
        }

        @Test
        @DisplayName("isCorporateEmail() returns false for personal email (gmail)")
        void whenPersonalGmail_thenReturnFalse() {
            assertThat(Email.of("user@gmail.com").isCorporateEmail()).isFalse();
        }
    }

    // Phone

    @Nested
    @DisplayName("Phone value object")
    class PhoneTests {

        @ParameterizedTest(name = "Valid phone: {0}")
        @ValueSource(strings = {
                "13800138000",
                "18612345678",
                "+8613800138000",
                "+14155552671",
        })
        @DisplayName("Valid phone number creates successfully")
        void whenValidPhone_thenCreateSuccessfully(String phone) {
            assertThat(Phone.of(phone)).isNotNull();
        }

        @ParameterizedTest(name = "Invalid phone: {0}")
        @ValueSource(strings = {
                "+0123456789",
                "abcdefghijk",
                "00000000000",
        })
        @DisplayName("Invalid phone number format throws DomainException")
        void whenInvalidPhone_thenThrowException(String phone) {
            assertThatThrownBy(() -> Phone.of(phone))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("Null phone number throws DomainException")
        void whenNullPhone_thenThrowException() {
            assertThatThrownBy(() -> Phone.of(null))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("Chinese mainland phone isChinaMobile() returns true")
        void whenChinaMobile_thenReturnTrue() {
            assertThat(Phone.of("13800138000").isChinaMobile()).isTrue();
        }

        @Test
        @DisplayName("International phone isChinaMobile() returns false")
        void whenInternational_thenReturnFalse() {
            assertThat(Phone.of("+14155552671").isChinaMobile()).isFalse();
        }

        @Test
        @DisplayName("toInternationalFormat() prepends +86 for Chinese mainland numbers")
        void whenChinaMobileToInternational_thenAddPrefix() {
            assertThat(Phone.of("13800138000").toInternationalFormat())
                    .isEqualTo("+8613800138000");
        }

        @Test
        @DisplayName("toMasked() masks 4 middle digits")
        void whenToMasked_thenHideMiddleDigits() {
            String masked = Phone.of("13800138000").toMasked();
            assertThat(masked).contains("****");
            assertThat(masked).startsWith("138");
            assertThat(masked).endsWith("8000");
        }
    }

    // Password

    @Nested
    @DisplayName("Password value object")
    class PasswordTests {

        @Test
        @DisplayName("Strong password (uppercase, lowercase, digits, special chars) creates successfully")
        void whenStrongPassword_thenCreateSuccessfully() {
            assertThat(Password.of("Password123!")).isNotNull();
        }

        @Test
        @DisplayName("Password shorter than 8 characters throws DomainException")
        void whenTooShort_thenThrowException() {
            assertThatThrownBy(() -> Password.of("Aa1!"))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("8");
        }

        @Test
        @DisplayName("Password exceeding 64 characters throws DomainException")
        void whenTooLong_thenThrowException() {
            String longPwd = "Aa1!" + "a".repeat(65);
            assertThatThrownBy(() -> Password.of(longPwd))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("Password without uppercase letter throws DomainException")
        void whenNoUppercase_thenThrowException() {
            assertThatThrownBy(() -> Password.of("password123!"))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("uppercase");
        }

        @Test
        @DisplayName("Password without lowercase letter throws DomainException")
        void whenNoLowercase_thenThrowException() {
            assertThatThrownBy(() -> Password.of("PASSWORD123!"))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("lowercase");
        }

        @Test
        @DisplayName("Password without digit throws DomainException")
        void whenNoDigit_thenThrowException() {
            assertThatThrownBy(() -> Password.of("PasswordABC!"))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("digit");
        }

        @Test
        @DisplayName("Null password throws DomainException")
        void whenNull_thenThrowException() {
            assertThatThrownBy(() -> Password.of(null))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("calculateStrength() returns > 60 when special characters are included")
        void whenSpecialCharsIncluded_thenHighStrength() {
            int strength = Password.of("Password123!").calculateStrength();
            assertThat(strength).isGreaterThan(60);
        }

        @Test
        @DisplayName("calculateStrength() stays within a reasonable range when only minimum requirements are met")
        void whenMinimalPassword_thenReasonableStrength() {
            int strength = Password.of("Password1").calculateStrength();
            assertThat(strength).isBetween(0, 100);
        }
    }

    // IpAddress

    @Nested
    @DisplayName("IpAddress value object")
    class IpAddressTests {

        @ParameterizedTest(name = "Valid IP: {0}")
        @ValueSource(strings = {
                "127.0.0.1",
                "192.168.1.100",
                "10.0.0.1",
                "255.255.255.255",
                "0.0.0.0",
        })
        @DisplayName("Valid IPv4 address creates successfully")
        void whenValidIpv4_thenCreateSuccessfully(String ip) {
            assertThat(IpAddress.of(ip)).isNotNull();
        }

        @Test
        @DisplayName("Null IP throws exception")
        void whenNull_thenThrowException() {
            assertThatThrownBy(() -> IpAddress.of(null))
                    .isInstanceOf(InvalidIpAddressException.class);
        }

        @Test
        @DisplayName("Blank IP string throws exception")
        void whenEmpty_thenThrowException() {
            assertThatThrownBy(() -> IpAddress.of(""))
                    .isInstanceOf(InvalidIpAddressException.class);
        }
    }

    // PermissionDigest

    @Nested
    @DisplayName("PermissionDigest value object")
    class PermissionDigestTests {

        @Test
        @DisplayName("Digests computed from the same permission set are equal (order-independent)")
        void whenSamePermissions_thenSameDigest() {
            Set<String> perms1 = Set.of("b", "a", "c");
            Set<String> perms2 = Set.of("c", "b", "a");
            PermissionDigest d1 = PermissionDigest.from(perms1);
            PermissionDigest d2 = PermissionDigest.from(perms2);
            assertThat(d1).isEqualTo(d2);
        }

        @Test
        @DisplayName("Digests computed from different permission sets are not equal")
        void whenDifferentPermissions_thenDifferentDigest() {
            PermissionDigest d1 = PermissionDigest.from(Set.of("user:read"));
            PermissionDigest d2 = PermissionDigest.from(Set.of("admin:write"));
            assertThat(d1).isNotEqualTo(d2);
        }

        @Test
        @DisplayName("Empty permission set returns the empty() digest")
        void whenEmptyPermissions_thenReturnEmptyDigest() {
            PermissionDigest digest = PermissionDigest.from(Set.of());
            assertThat(digest).isEqualTo(PermissionDigest.empty());
        }

        @Test
        @DisplayName("matches() returns true when permission sets match")
        void whenMatchingPermissions_thenReturnTrue() {
            Set<String> perms = Set.of("user:read", "user:write");
            PermissionDigest digest = PermissionDigest.from(perms);
            assertThat(digest.matches(perms)).isTrue();
        }

        @Test
        @DisplayName("matches() returns false when permission sets differ")
        void whenNotMatchingPermissions_thenReturnFalse() {
            PermissionDigest digest = PermissionDigest.from(Set.of("user:read"));
            assertThat(digest.matches(Set.of("admin:write"))).isFalse();
        }

        @Test
        @DisplayName("shortValue() returns the first 8 characters of the digest")
        void whenShortValue_thenReturnFirst8Chars() {
            PermissionDigest digest = PermissionDigest.from(Set.of("user:read"));
            assertThat(digest.shortValue()).hasSize(8);
            assertThat(digest.value()).startsWith(digest.shortValue());
        }

        @Test
        @DisplayName("PermissionDigest construction with non-MD5 string throws IllegalArgumentException")
        void whenInvalidFormat_thenThrowException() {
            assertThatThrownBy(() -> new PermissionDigest("not-a-md5"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("PermissionDigest construction with blank string throws IllegalArgumentException")
        void whenBlankValue_thenThrowException() {
            assertThatThrownBy(() -> new PermissionDigest(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
