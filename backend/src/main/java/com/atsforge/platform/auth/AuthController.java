package com.atsforge.platform.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.atsforge.platform.auth.AuthDtos.AuthResponse;
import com.atsforge.platform.auth.AuthDtos.ForgotPasswordRequest;
import com.atsforge.platform.auth.AuthDtos.LoginRequest;
import com.atsforge.platform.auth.AuthDtos.MessageResponse;
import com.atsforge.platform.auth.AuthDtos.RefreshRequest;
import com.atsforge.platform.auth.AuthDtos.RegisterRequest;
import com.atsforge.platform.auth.AuthDtos.ResetPasswordRequest;
import com.atsforge.platform.auth.AuthDtos.TokenRequest;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an email/password account and send email verification")
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/verify-email")
    public MessageResponse verify(@Valid @RequestBody TokenRequest request) {
        return authService.verifyEmail(request.token());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest);
    }

    @PostMapping("/logout")
    public MessageResponse logout(@Valid @RequestBody RefreshRequest request) {
        return authService.logout(request.refreshToken());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        return authService.refresh(request.refreshToken(), httpRequest);
    }

    @PostMapping("/oauth/exchange")
    public AuthResponse exchange(@Valid @RequestBody TokenRequest request, HttpServletRequest httpRequest) {
        return authService.exchangeOAuthCode(request.token(), httpRequest);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public MessageResponse reset(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }
}
