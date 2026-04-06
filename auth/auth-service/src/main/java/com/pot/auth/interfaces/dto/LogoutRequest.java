package com.pot.auth.interfaces.dto;

/**
 * Logout request payload.
 */
public record LogoutRequest(
                /**
                 * Optional refresh token revoked together with the access token.
                 */
                String refreshToken) {
}
