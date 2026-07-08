package com.arjunsports.contentagent.modules.instagram.entity;

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

/**
 * A connected Instagram Business Account. {@code accessTokenEncrypted} is
 * ciphertext at rest (see {@link com.arjunsports.contentagent.common.util.CredentialEncryptionUtil})
 * and is never included in any API response - only the service layer decrypts
 * it, immediately before handing it to an {@link com.arjunsports.contentagent.modules.instagram.provider.InstagramPublisher}.
 * Disconnecting sets {@code active = false} rather than deleting the row, so
 * the connection/disconnection history survives (mirrors {@code ContentDraft}'s
 * soft-delete convention).
 */
@Getter
@Setter
@Entity
@Table(name = "instagram_accounts")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstagramAccount extends BaseEntity {

    @Column(name = "business_account_id", nullable = false)
    private String businessAccountId;

    @Column(name = "facebook_page_id")
    private String facebookPageId;

    @Column(name = "username")
    private String username;

    @Column(name = "access_token_encrypted", columnDefinition = "TEXT", nullable = false)
    private String accessTokenEncrypted;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt;

    @Column(name = "disconnected_at")
    private Instant disconnectedAt;
}
