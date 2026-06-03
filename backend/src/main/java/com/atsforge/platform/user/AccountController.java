package com.atsforge.platform.user;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.atsforge.platform.auth.AuthService;
import com.atsforge.platform.security.SecurityUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {
    private final UserRepository users;
    private final AuthService authService;

    public AccountController(UserRepository users, AuthService authService) {
        this.users = users;
        this.authService = authService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public AccountResponse me() {
        UserEntity user = users.findById(SecurityUtils.currentUserId()).orElseThrow();
        return response(user);
    }

    @PatchMapping
    @Transactional
    public AccountResponse update(@Valid @RequestBody AccountUpdate request) {
        UserEntity user = users.findById(SecurityUtils.currentUserId()).orElseThrow();
        user.setDisplayName(request.displayName().trim());
        user.setLocale(request.locale());
        return response(user);
    }

    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteAccount() {
        authService.deleteAccount(SecurityUtils.currentUserId());
    }

    private AccountResponse response(UserEntity user) {
        return new AccountResponse(user.getEmail(), user.getDisplayName(), user.getRole(), user.getLocale(), user.isEmailVerified());
    }

    public record AccountUpdate(@NotBlank @Size(max = 120) String displayName, @NotBlank @Size(max = 12) String locale) {}
    public record AccountResponse(String email, String displayName, UserRole role, String locale, boolean emailVerified) {}
}

