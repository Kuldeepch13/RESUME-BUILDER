package com.atsforge.platform.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atsforge.platform.auth.AuthDtos.AuthResponse;
import com.atsforge.platform.auth.AuthDtos.ForgotPasswordRequest;
import com.atsforge.platform.auth.AuthDtos.LoginRequest;
import com.atsforge.platform.auth.AuthDtos.MessageResponse;
import com.atsforge.platform.auth.AuthDtos.RegisterRequest;
import com.atsforge.platform.auth.AuthDtos.ResetPasswordRequest;
import com.atsforge.platform.auth.AuthDtos.UserResponse;
import com.atsforge.platform.common.ConflictException;
import com.atsforge.platform.common.ForbiddenException;
import com.atsforge.platform.config.AppProperties;
import com.atsforge.platform.security.JwtService;
import com.atsforge.platform.user.UserEntity;
import com.atsforge.platform.user.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService {
    private final UserRepository users;
    private final AuthTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenHashingService hashing;
    private final JwtService jwt;
    private final AppProperties properties;
    private final AccountMailService mail;

    public AuthService(UserRepository users, AuthTokenRepository tokens, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, TokenHashingService hashing, JwtService jwt,
                       AppProperties properties, AccountMailService mail) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.hashing = hashing;
        this.jwt = jwt;
        this.properties = properties;
        this.mail = mail;
    }

    @Transactional
    public MessageResponse register(RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account already exists for this email.");
        }
        UserEntity user = users.save(new UserEntity(
                request.email(), passwordEncoder.encode(request.password()), request.displayName().trim()));
        String rawToken = createToken(user, AuthTokenEntity.TokenType.EMAIL_VERIFICATION, Instant.now().plus(24, ChronoUnit.HOURS), null);
        mail.sendVerification(user.getEmail(), rawToken);
        return new MessageResponse("Registration successful. Check your email to verify the account.");
    }

    @Transactional
    public MessageResponse verifyEmail(String rawToken) {
        AuthTokenEntity token = usable(rawToken, AuthTokenEntity.TokenType.EMAIL_VERIFICATION);
        token.getUser().verifyEmail();
        token.consume();
        return new MessageResponse("Email address verified. You can now sign in.");
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        UserEntity user = users.findByEmailIgnoreCase(request.email()).orElseThrow();
        if (!user.isEmailVerified()) {
            throw new ForbiddenException("Verify your email before signing in.");
        }
        return issueTokens(user, httpRequest);
    }

    @Transactional
    public AuthResponse refresh(String rawRefresh, HttpServletRequest request) {
        AuthTokenEntity previous = usable(rawRefresh, AuthTokenEntity.TokenType.REFRESH);
        previous.consume();
        return issueTokens(previous.getUser(), request);
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        users.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            String raw = createToken(user, AuthTokenEntity.TokenType.PASSWORD_RESET, Instant.now().plus(30, ChronoUnit.MINUTES), null);
            mail.sendReset(user.getEmail(), raw);
        });
        return new MessageResponse("If an account exists, a password reset email has been sent.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        AuthTokenEntity token = usable(request.token(), AuthTokenEntity.TokenType.PASSWORD_RESET);
        token.getUser().setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.consume();
        return new MessageResponse("Password updated successfully.");
    }

    @Transactional
    public String createOAuthExchange(UserEntity user) {
        return createToken(user, AuthTokenEntity.TokenType.OAUTH_EXCHANGE, Instant.now().plus(2, ChronoUnit.MINUTES), null);
    }

    @Transactional
    public AuthResponse exchangeOAuthCode(String rawCode, HttpServletRequest request) {
        AuthTokenEntity exchange = usable(rawCode, AuthTokenEntity.TokenType.OAUTH_EXCHANGE);
        exchange.consume();
        return issueTokens(exchange.getUser(), request);
    }

    @Transactional
    public MessageResponse logout(String rawRefreshToken) {
        AuthTokenEntity token = tokens.findByTokenHashAndTokenType(hashing.hash(rawRefreshToken), AuthTokenEntity.TokenType.REFRESH)
                .orElseThrow(() -> new ForbiddenException("Token is invalid or expired."));
        token.consume();
        return new MessageResponse("Logged out successfully.");
    }

    @Transactional
    public MessageResponse revokeAllTokens(java.util.UUID userId) {
        tokens.findByUserIdAndTokenType(userId, AuthTokenEntity.TokenType.REFRESH).forEach(AuthTokenEntity::consume);
        return new MessageResponse("All sessions revoked.");
    }

    @Transactional
    public void deleteAccount(java.util.UUID userId) {
        UserEntity user = users.findById(userId).orElseThrow();
        tokens.deleteByUserId(userId);
        users.delete(user);
    }

    private AuthResponse issueTokens(UserEntity user, HttpServletRequest request) {
        String refresh = createToken(user, AuthTokenEntity.TokenType.REFRESH,
                Instant.now().plus(properties.jwt().refreshTokenDuration()), request);
        return new AuthResponse(jwt.accessToken(user), refresh, properties.jwt().accessTokenDuration().toSeconds(),
                new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.isEmailVerified()));
    }

    private String createToken(UserEntity user, AuthTokenEntity.TokenType type, Instant expiry, HttpServletRequest request) {
        String raw = hashing.newToken();
        String device = request == null ? null : request.getHeader("User-Agent");
        String ip = request == null ? null : request.getRemoteAddr();
        tokens.save(new AuthTokenEntity(user, hashing.hash(raw), type, expiry, device, ip));
        return raw;
    }

    private AuthTokenEntity usable(String raw, AuthTokenEntity.TokenType type) {
        AuthTokenEntity token = tokens.findByTokenHashAndTokenType(hashing.hash(raw), type)
                .orElseThrow(() -> new ForbiddenException("Token is invalid or expired."));
        if (!token.isUsable()) {
            throw new ForbiddenException("Token is invalid or expired.");
        }
        return token;
    }
}

