package com.arjunsports.contentagent.modules.auth;

import com.arjunsports.contentagent.modules.auth.dto.ChangePasswordRequest;
import com.arjunsports.contentagent.modules.auth.dto.ResetPasswordRequest;
import com.arjunsports.contentagent.modules.user.dto.UserResponse;

import java.util.UUID;

public interface AuthService {

    AuthResult login(String email, String password, String clientIp);

    AuthResult refresh(String rawRefreshToken);

    void logout(String rawRefreshToken);

    UserResponse getCurrentUser(UUID userId);

    AuthResult changePassword(UUID userId, ChangePasswordRequest request);

    void forgotPassword(String email);

    void resetPassword(ResetPasswordRequest request);

    record AuthResult(String accessToken, String refreshToken, UserResponse user) {
    }
}
