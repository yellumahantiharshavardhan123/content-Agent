package com.arjunsports.contentagent.modules.auth;

import java.util.UUID;

public interface RefreshTokenService {

    /** Issues a new refresh token for the user and returns the raw (unhashed) value to set in a cookie. */
    String issue(UUID userId);

    /**
     * Validates and rotates a presented refresh token: the old token is revoked and a new one issued.
     * Presenting an already-revoked token is treated as possible theft and revokes the entire session family.
     */
    RotatedToken rotate(String rawToken);

    /** Revokes a single refresh token (logout of the current session). */
    void revoke(String rawToken);

    /** Revokes every active refresh token for a user (logout everywhere / password change). */
    void revokeAllForUser(UUID userId);

    record RotatedToken(String rawToken, UUID userId) {
    }
}
