package com.arjunsports.contentagent.modules.auth;

import com.arjunsports.contentagent.common.dto.ApiResponse;
import com.arjunsports.contentagent.common.exception.UnauthorizedException;
import com.arjunsports.contentagent.modules.auth.dto.ChangePasswordRequest;
import com.arjunsports.contentagent.modules.auth.dto.ForgotPasswordRequest;
import com.arjunsports.contentagent.modules.auth.dto.LoginRequest;
import com.arjunsports.contentagent.modules.auth.dto.ResetPasswordRequest;
import com.arjunsports.contentagent.modules.user.UserPrincipal;
import com.arjunsports.contentagent.modules.user.dto.UserResponse;
import com.arjunsports.contentagent.security.CookieUtil;
import com.arjunsports.contentagent.security.LoginRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final LoginRateLimiter loginRateLimiter;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        String clientIp = clientIp(httpRequest);
        if (!loginRateLimiter.tryConsume(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("Too many login attempts. Please try again in a minute."));
        }

        AuthService.AuthResult result = authService.login(request.email(), request.password(), clientIp);
        return withTokenCookies(result, "Login successful");
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<UserResponse>> refresh(HttpServletRequest httpRequest) {
        String rawRefreshToken = cookieUtil.extractRefreshToken(httpRequest)
                .orElseThrow(() -> new UnauthorizedException("No refresh token present"));

        AuthService.AuthResult result = authService.refresh(rawRefreshToken);
        return withTokenCookies(result, "Token refreshed");
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest) {
        cookieUtil.extractRefreshToken(httpRequest).ifPresent(authService::logout);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.expireAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.expireRefreshTokenCookie().toString())
                .body(ApiResponse.success("Logged out", null));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(authService.getCurrentUser(principal.getId()));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody ChangePasswordRequest request) {

        AuthService.AuthResult result = authService.changePassword(principal.getId(), request);
        return withTokenCookies(result, "Password changed successfully");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ApiResponse.success(
                "If an account exists for this email, password reset instructions will be sent", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success("Password reset successfully", null);
    }

    private ResponseEntity<ApiResponse<UserResponse>> withTokenCookies(AuthService.AuthResult result, String message) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildAccessTokenCookie(result.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookieUtil.buildRefreshTokenCookie(result.refreshToken()).toString())
                .body(ApiResponse.success(message, result.user()));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
