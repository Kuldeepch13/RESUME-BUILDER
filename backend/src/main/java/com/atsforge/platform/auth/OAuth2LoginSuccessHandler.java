package com.atsforge.platform.auth;

import com.atsforge.platform.config.AppProperties;
import com.atsforge.platform.user.AuthProvider;
import com.atsforge.platform.user.UserEntity;
import com.atsforge.platform.user.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final UserRepository users;
    private final AuthService authService;
    private final AppProperties properties;

    public OAuth2LoginSuccessHandler(UserRepository users, AuthService authService, AppProperties properties) {
        this.users = users;
        this.authService = authService;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        UserEntity user = users.findByEmailIgnoreCase(email).orElseGet(() -> {
            UserEntity created = new UserEntity(email, null, name == null ? email : name);
            created.setAuthProvider(AuthProvider.GOOGLE);
            created.verifyEmail();
            return users.save(created);
        });
        if (!user.isEmailVerified()) {
            user.verifyEmail();
        }
        String code = authService.createOAuthExchange(user);
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        response.sendRedirect(properties.publicUrl() + "/auth/callback?code=" +
                URLEncoder.encode(code, StandardCharsets.UTF_8));
    }
}
