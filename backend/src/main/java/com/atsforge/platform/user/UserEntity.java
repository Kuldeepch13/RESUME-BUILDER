package com.atsforge.platform.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 320)
    private String email;
    @Column(name = "password_hash")
    private String passwordHash;
    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role = UserRole.USER;
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 30)
    private AuthProvider authProvider = AuthProvider.LOCAL;
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(nullable = false, length = 12)
    private String locale = "en-US";
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEntity() {}

    public UserEntity(String email, String passwordHash, String displayName) {
        this.email = email.toLowerCase();
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public UserRole getRole() { return role; }
    public boolean isEmailVerified() { return emailVerified; }
    public boolean isEnabled() { return enabled; }
    public String getLocale() { return locale; }
    public Instant getCreatedAt() { return createdAt; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void verifyEmail() { this.emailVerified = true; }
    public void setRole(UserRole role) { this.role = role; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setLocale(String locale) { this.locale = locale; }
    public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }
}
