package com.atsforge.platform.security;

import java.lang.reflect.Field;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import com.atsforge.platform.config.AppProperties;
import com.atsforge.platform.user.UserEntity;

class JwtServiceTest {
    private static AppProperties createProperties() {
        return new AppProperties(
                URI.create("http://localhost"),
                "http://localhost",
                new AppProperties.Jwt("atsforge", "01234567890123456789012345678901", Duration.ofMinutes(15), Duration.ofDays(30)),
                new AppProperties.OpenAi("test-key", "gpt-4o", URI.create("https://api.openai.com")),
                new AppProperties.Stripe("sk_test", "whsec_test", "price_monthly", "price_yearly"),
                new AppProperties.Storage(URI.create("https://storage.example.com"), "us-east-1", "bucket", "accessKey", "secretKey"),
                new AppProperties.Limits(100, 20, 3)
        );
    }

    private static UserEntity createUser() throws Exception {
        UserEntity user = new UserEntity("tester@example.com", "password-hash", "Tester");
        Field idField = UserEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return user;
    }

    @Test
    void accessTokenContainsClaims() throws Exception {
        JwtService service = new JwtService(createProperties());
        UserEntity user = createUser();

        String token = service.accessToken(user);
        assertThat(token).isNotBlank();

        assertThat(service.subject(token)).isEqualTo(user.getId());
        assertThat(service.parse(token).get("email", String.class)).isEqualTo(user.getEmail());
        assertThat(service.parse(token).get("role", String.class)).isEqualTo(user.getRole().name());
    }

    @Test
    void authenticateReturnsPrincipalForValidToken() throws Exception {
        JwtService service = new JwtService(createProperties());
        UserEntity user = createUser();

        String token = service.accessToken(user);
        Authentication auth = service.authenticate(token);

        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo(user.getEmail());
        assertThat(auth.getAuthorities()).extracting("authority").contains("ROLE_USER");
    }

    @Test
    void authenticateReturnsNullForInvalidToken() {
        JwtService service = new JwtService(createProperties());
        Authentication auth = service.authenticate("invalid-token");

        assertThat(auth).isNull();
    }
}
