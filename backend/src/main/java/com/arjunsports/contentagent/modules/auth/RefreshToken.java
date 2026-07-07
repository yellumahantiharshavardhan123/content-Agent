package com.arjunsports.contentagent.modules.auth;

import com.arjunsports.contentagent.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** SHA-256 hex digest of the raw token; the raw value is never persisted. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_token_hash", length = 128)
    private String replacedByTokenHash;

    public boolean isActive() {
        return !revoked && expiresAt.isAfter(Instant.now());
    }
}
