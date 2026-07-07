package com.arjunsports.contentagent.modules.auth;

import com.arjunsports.contentagent.common.audit.ActivityAction;
import com.arjunsports.contentagent.common.audit.AuditLogService;
import com.arjunsports.contentagent.common.exception.BadRequestException;
import com.arjunsports.contentagent.common.exception.ResourceNotFoundException;
import com.arjunsports.contentagent.common.exception.UnauthorizedException;
import com.arjunsports.contentagent.common.notification.NotificationService;
import com.arjunsports.contentagent.common.notification.NotificationType;
import com.arjunsports.contentagent.common.util.SecureTokenUtil;
import com.arjunsports.contentagent.modules.auth.dto.ChangePasswordRequest;
import com.arjunsports.contentagent.modules.auth.dto.ResetPasswordRequest;
import com.arjunsports.contentagent.modules.user.User;
import com.arjunsports.contentagent.modules.user.UserPrincipal;
import com.arjunsports.contentagent.modules.user.UserRepository;
import com.arjunsports.contentagent.modules.user.UserStatus;
import com.arjunsports.contentagent.modules.user.dto.UserResponse;
import com.arjunsports.contentagent.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public AuthResult login(String email, String password, String clientIp) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));
        } catch (BadCredentialsException | DisabledException | LockedException ex) {
            auditLogService.record(ActivityAction.LOGIN_FAILED, "User", null,
                    Map.of("email", email, "clientIp", clientIp == null ? "" : clientIp));
            throw new UnauthorizedException("Invalid email or password");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = findUserOrThrow(principal.getId());
        user.setLastLogin(Instant.now());
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = refreshTokenService.issue(user.getId());

        auditLogService.record(ActivityAction.LOGIN, "User", user.getId(), Map.of("clientIp", clientIp == null ? "" : clientIp));
        notificationService.notify(user.getId(), NotificationType.LOGIN_SUCCESS, "Login successful",
                "You logged in at " + Instant.now(), null);

        return new AuthResult(accessToken, refreshToken, UserResponse.from(user));
    }

    @Override
    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedToken rotated = refreshTokenService.rotate(rawRefreshToken);
        User user = findUserOrThrow(rotated.userId());

        if (!user.isActive() || user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is no longer active");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResult(accessToken, rotated.rawToken(), UserResponse.from(user));
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        UUID userId = currentUserId();
        refreshTokenService.revoke(rawRefreshToken);
        auditLogService.record(ActivityAction.LOGOUT, "User", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        return UserResponse.from(findUserOrThrow(userId));
    }

    @Override
    @Transactional
    public AuthResult changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findUserOrThrow(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Revoke every session (including this one) then issue a fresh pair so this device stays logged in.
        refreshTokenService.revokeAllForUser(userId);
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = refreshTokenService.issue(user.getId());

        auditLogService.record(ActivityAction.PASSWORD_CHANGE, "User", userId);
        notificationService.notify(userId, NotificationType.PASSWORD_CHANGED, "Password changed",
                "Your password was changed at " + Instant.now(), null);

        return new AuthResult(accessToken, refreshToken, UserResponse.from(user));
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String rawToken = SecureTokenUtil.generate();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .userId(user.getId())
                    .tokenHash(SecureTokenUtil.hash(rawToken))
                    .expiresAt(Instant.now().plus(java.time.Duration.ofHours(1)))
                    .build();
            passwordResetTokenRepository.save(resetToken);

            // Email delivery is not implemented yet (see Module 1 scope) - the token is persisted and
            // ready to be emailed once a real EmailNotificationService lands. Never log the raw token.
            log.info("Password reset requested for user {}; email delivery not yet implemented", user.getId());
        });
        // Always behave identically whether or not the email exists, to avoid user enumeration.
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(SecureTokenUtil.hash(request.token()))
                .filter(PasswordResetToken::isActive)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        User user = findUserOrThrow(resetToken.getUserId());
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        refreshTokenService.revokeAllForUser(user.getId());

        auditLogService.record(ActivityAction.PASSWORD_CHANGE, "User", user.getId());
        notificationService.notify(user.getId(), NotificationType.PASSWORD_CHANGED, "Password changed",
                "Your password was reset at " + Instant.now(), null);
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        throw new UnauthorizedException("No authenticated user in context");
    }
}
