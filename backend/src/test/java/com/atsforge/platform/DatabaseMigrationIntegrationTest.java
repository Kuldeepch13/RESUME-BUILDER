package com.atsforge.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.atsforge.platform.template.TemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = { "spring.cache.type=none", "spring.flyway.enabled=true" })
class DatabaseMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("atsforge").withUsername("atsforge").withPassword("integration-test");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    TemplateRepository templates;

    @Test
    void migratesCoreSchemaAndSeedsTemplateCatalog() {
        assertThat(templates.findByCodeAndActiveTrue("atlas")).isPresent();
        assertThat(templates.findAllByActiveTrueOrderByPremiumAscNameAsc()).hasSizeGreaterThanOrEqualTo(3);
    }
}
