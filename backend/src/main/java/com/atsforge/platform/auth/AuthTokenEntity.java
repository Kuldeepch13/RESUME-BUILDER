package com.atsforge.platform.auth;

import com.atsforge.platform.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_tokens")
public class AuthTokenEntity {
    public enum TokenType { REFRESH, EMAIL_VERIFICATION, PASSWORD_RESET, OAUTH_EXCHANGE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 30)
    private TokenType tokenType;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "consumed_at")
    private Instant consumedAt;
    @Column(name = "device_info", length = 255)
    private String deviceInfo;
    @Column(name = "ip_address", length = 64)
    private String ipAddress;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuthTokenEntity() {}

    public AuthTokenEntity(UserEntity user, String tokenHash, TokenType type, Instant expiresAt, String deviceInfo, String ipAddress) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.tokenType = type;
        this.expiresAt = expiresAt;
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
    }

    @PrePersist
    void create() { createdAt = Instant.now(); }
    public UserEntity getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsable() { return consumedAt == null && expiresAt.isAfter(Instant.now()); }
    public void consume() { consumedAt = Instant.now(); }
}
