package com.arjunsports.contentagent.modules.auth;

import com.arjunsports.contentagent.common.exception.UnauthorizedException;
import com.arjunsports.contentagent.common.util.SecureTokenUtil;
import com.arjunsports.contentagent.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private RefreshTokenServiceImpl refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository, jwtTokenProvider);
    }

    @Test
    void issue_persistsHashedTokenAndReturnsRawValue() {
        when(jwtTokenProvider.getRefreshTokenTtl()).thenReturn(Duration.ofDays(7));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        UUID userId = UUID.randomUUID();

        String raw = refreshTokenService.issue(userId);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(raw).isNotBlank();
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getTokenHash()).isEqualTo(SecureTokenUtil.hash(raw));
        assertThat(captor.getValue().getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void rotate_activeToken_revokesOldAndIssuesNew() {
        String rawOldToken = "old-raw-token";
        UUID userId = UUID.randomUUID();
        RefreshToken existing = RefreshToken.builder()
                .userId(userId)
                .tokenHash(SecureTokenUtil.hash(rawOldToken))
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(SecureTokenUtil.hash(rawOldToken)))
                .thenReturn(Optional.of(existing));
        when(jwtTokenProvider.getRefreshTokenTtl()).thenReturn(Duration.ofDays(7));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.RotatedToken rotated = refreshTokenService.rotate(rawOldToken);

        assertThat(rotated.userId()).isEqualTo(userId);
        assertThat(rotated.rawToken()).isNotEqualTo(rawOldToken);
        assertThat(existing.isRevoked()).isTrue();
        assertThat(existing.getReplacedByTokenHash()).isEqualTo(SecureTokenUtil.hash(rotated.rawToken()));
    }

    @Test
    void rotate_revokedTokenReused_revokesEntireFamilyAndThrows() {
        String rawToken = "already-used-token";
        UUID userId = UUID.randomUUID();
        RefreshToken revokedToken = RefreshToken.builder()
                .userId(userId)
                .tokenHash(SecureTokenUtil.hash(rawToken))
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByTokenHash(SecureTokenUtil.hash(rawToken)))
                .thenReturn(Optional.of(revokedToken));
        when(refreshTokenRepository.findByUserIdAndRevokedFalse(userId)).thenReturn(List.of());

        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                .isInstanceOf(UnauthorizedException.class);

        verify(refreshTokenRepository).findByUserIdAndRevokedFalse(userId);
    }

    @Test
    void rotate_expiredToken_throwsUnauthorized() {
        String rawToken = "expired-token";
        RefreshToken expired = RefreshToken.builder()
                .userId(UUID.randomUUID())
                .tokenHash(SecureTokenUtil.hash(rawToken))
                .expiresAt(Instant.now().minusSeconds(60))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(SecureTokenUtil.hash(rawToken))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void rotate_unknownToken_throwsUnauthorized() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("unknown"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void revokeAllForUser_marksEveryActiveTokenRevoked() {
        UUID userId = UUID.randomUUID();
        RefreshToken t1 = RefreshToken.builder().userId(userId).revoked(false).build();
        RefreshToken t2 = RefreshToken.builder().userId(userId).revoked(false).build();
        when(refreshTokenRepository.findByUserIdAndRevokedFalse(userId)).thenReturn(List.of(t1, t2));
        when(refreshTokenRepository.saveAll(any())).thenReturn(List.of(t1, t2));

        refreshTokenService.revokeAllForUser(userId);

        assertThat(t1.isRevoked()).isTrue();
        assertThat(t2.isRevoked()).isTrue();
    }
}
