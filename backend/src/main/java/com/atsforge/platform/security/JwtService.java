package com.atsforge.platform.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.atsforge.platform.config.AppProperties;
import com.atsforge.platform.user.UserEntity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private final AppProperties properties;
    private final SecretKey key;

    public JwtService(AppProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
    }

    public String accessToken(UserEntity user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.jwt().issuer())
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.jwt().accessTokenDuration())))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).requireIssuer(properties.jwt().issuer()).build()
                .parseSignedClaims(token).getPayload();
    }

    public UUID subject(String token) {
        return UUID.fromString(parse(token).getSubject());
    }

    public Authentication authenticate(String token) {
        try {
            Claims claims = parse(token);
            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);
            java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities = 
                    java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
            UserPrincipal principal = new UserPrincipal(userId, email, null, true, authorities);
            return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        } catch (Exception ex) {
            return null;
        }
    }
}

