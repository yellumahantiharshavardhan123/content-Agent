package com.arjunsports.contentagent.modules.auth;

import com.arjunsports.contentagent.common.audit.ActivityAction;
import com.arjunsports.contentagent.common.audit.AuditLogService;
import com.arjunsports.contentagent.common.exception.BadRequestException;
import com.arjunsports.contentagent.common.exception.UnauthorizedException;
import com.arjunsports.contentagent.common.notification.NotificationService;
import com.arjunsports.contentagent.common.notification.NotificationType;
import com.arjunsports.contentagent.modules.auth.dto.ChangePasswordRequest;
import com.arjunsports.contentagent.modules.user.Role;
import com.arjunsports.contentagent.modules.user.User;
import com.arjunsports.contentagent.modules.user.UserPrincipal;
import com.arjunsports.contentagent.modules.user.UserRepository;
import com.arjunsports.contentagent.modules.user.UserStatus;
import com.arjunsports.contentagent.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(authenticationManager, userRepository, passwordEncoder, jwtTokenProvider,
                refreshTokenService, passwordResetTokenRepository, auditLogService, notificationService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User activeAdmin(UUID id) {
        User user = User.builder()
                .firstName("Ada")
                .lastName("Min")
                .email("admin@example.com")
                .password("{bcrypt}hashed")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .active(true)
                .build();
        user.setId(id);
        return user;
    }

    @Test
    void login_success_issuesTokensAndAudits() {
        UUID userId = UUID.randomUUID();
        User user = activeAdmin(userId);
        UserPrincipal principal = new UserPrincipal(user);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(userId, user.getEmail(), "ADMIN")).thenReturn("access-token");
        when(refreshTokenService.issue(userId)).thenReturn("refresh-token");

        AuthService.AuthResult result = authService.login(user.getEmail(), "correct-password", "127.0.0.1");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.user().email()).isEqualTo(user.getEmail());
        assertThat(user.getLastLogin()).isNotNull();

        verify(auditLogService).record(eq(ActivityAction.LOGIN), eq("User"), eq(userId), any());
        verify(notificationService).notify(eq(userId), eq(NotificationType.LOGIN_SUCCESS), anyString(), anyString(), eq(null));
    }

    @Test
    void login_wrongPassword_throwsUnauthorizedAndAuditsFailure() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad creds"));

        assertThatThrownBy(() -> authService.login("admin@example.com", "wrong-password", "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class);

        verify(auditLogService).record(eq(ActivityAction.LOGIN_FAILED), eq("User"), eq(null), any());
        verify(notificationService, never()).notify(any(), any(), anyString(), anyString(), any());
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsBadRequest() {
        UUID userId = UUID.randomUUID();
        User user = activeAdmin(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-current", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(userId,
                new ChangePasswordRequest("wrong-current", "NewPassw0rd")))
                .isInstanceOf(BadRequestException.class);

        verify(refreshTokenService, never()).revokeAllForUser(any());
    }

    @Test
    void changePassword_success_revokesAllSessionsAndIssuesFreshTokens() {
        UUID userId = UUID.randomUUID();
        User user = activeAdmin(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-pass", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("NewPassw0rd")).thenReturn("{bcrypt}new-hash");
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("new-access-token");
        when(refreshTokenService.issue(userId)).thenReturn("new-refresh-token");

        AuthService.AuthResult result = authService.changePassword(userId,
                new ChangePasswordRequest("current-pass", "NewPassw0rd"));

        assertThat(user.getPassword()).isEqualTo("{bcrypt}new-hash");
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenService, times(1)).revokeAllForUser(userId);
        verify(auditLogService).record(ActivityAction.PASSWORD_CHANGE, "User", userId);
        verify(notificationService).notify(eq(userId), eq(NotificationType.PASSWORD_CHANGED), anyString(), anyString(), eq(null));
    }
}
