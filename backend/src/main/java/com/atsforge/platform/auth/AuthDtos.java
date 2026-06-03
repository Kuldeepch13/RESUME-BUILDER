package com.atsforge.platform.auth;

import com.atsforge.platform.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 12, max = 128) String password,
            @NotBlank @Size(max = 120) String displayName) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record TokenRequest(@NotBlank String token) {}
    public record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 12, max = 128) String newPassword) {}
    public record ForgotPasswordRequest(@NotBlank @Email String email) {}
    public record MessageResponse(String message) {}
    public record UserResponse(UUID id, String email, String displayName, UserRole role, boolean emailVerified) {}
    public record AuthResponse(String accessToken, String refreshToken, long expiresInSeconds, UserResponse user) {}
}

