package com.atsforge.platform.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthTokenRepository extends JpaRepository<AuthTokenEntity, UUID> {
    Optional<AuthTokenEntity> findByTokenHashAndTokenType(String tokenHash, AuthTokenEntity.TokenType tokenType);
    List<AuthTokenEntity> findByUserIdAndTokenType(UUID userId, AuthTokenEntity.TokenType tokenType);
    void deleteByUserId(UUID userId);
}
