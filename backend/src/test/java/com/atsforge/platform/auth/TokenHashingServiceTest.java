package com.atsforge.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenHashingServiceTest {
    private final TokenHashingService service = new TokenHashingService();

    @Test
    void generatesUnpredictableTokensAndStoresOnlyStableDigests() {
        String first = service.newToken();
        String second = service.newToken();

        assertThat(first).isNotEqualTo(second).hasSizeGreaterThan(40);
        assertThat(service.hash(first)).hasSize(64).isEqualTo(service.hash(first)).doesNotContain(first);
    }
}

