package com.arjunsports.contentagent.modules.auth;

import com.arjunsports.contentagent.common.exception.UnauthorizedException;
import com.arjunsports.contentagent.common.util.SecureTokenUtil;
import com.arjunsports.contentagent.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public String issue(UUID userId) {
        String raw = SecureTokenUtil.generate();
        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .tokenHash(SecureTokenUtil.hash(raw))
                .expiresAt(Instant.now().plus(jwtTokenProvider.getRefreshTokenTtl()))
                .build();
        refreshTokenRepository.save(token);
        return raw;
    }

    @Override
    @Transactional
    public RotatedToken rotate(String rawToken) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(SecureTokenUtil.hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (existing.isRevoked()) {
            // A rotated-out token was presented again: likely theft/replay. Kill the whole session family.
            revokeAllForUser(existing.getUserId());
            throw new UnauthorizedException("Refresh token has already been used; all sessions have been revoked");
        }

        if (!existing.isActive()) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        String newRaw = SecureTokenUtil.generate();
        String newHash = SecureTokenUtil.hash(newRaw);

        existing.setRevoked(true);
        existing.setRevokedAt(Instant.now());
        existing.setReplacedByTokenHash(newHash);
        refreshTokenRepository.save(existing);

        RefreshToken next = RefreshToken.builder()
                .userId(existing.getUserId())
                .tokenHash(newHash)
                .expiresAt(Instant.now().plus(jwtTokenProvider.getRefreshTokenTtl()))
                .build();
        refreshTokenRepository.save(next);

        return new RotatedToken(newRaw, existing.getUserId());
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(SecureTokenUtil.hash(rawToken)).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    @Override
    @Transactional
    public void revokeAllForUser(UUID userId) {
        List<RefreshToken> active = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        Instant now = Instant.now();
        active.forEach(token -> {
            token.setRevoked(true);
            token.setRevokedAt(now);
        });
        refreshTokenRepository.saveAll(active);
    }
}
