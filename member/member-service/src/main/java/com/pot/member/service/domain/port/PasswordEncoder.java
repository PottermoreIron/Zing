package com.pot.member.service.domain.port;

/**
 * Outbound port for password hashing and verification.
 *
 * <p>
 * The domain layer uses this abstraction to stay decoupled from Spring Security
 * or any other
 * concrete hashing library.
 * </p>
 *
 * @author Pot
 * @since 2026-03-18
 */
public interface PasswordEncoder {

    /**
     * Encode a raw password.
     */
    String encode(String rawPassword);

    /**
     * Check whether a raw password matches its encoded value.
     */
    boolean matches(String rawPassword, String encodedPassword);
}
