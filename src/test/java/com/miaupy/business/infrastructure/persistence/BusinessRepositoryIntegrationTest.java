package com.miaupy.business.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.miaupy.business.domain.Business;
import com.miaupy.business.domain.BusinessRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = "spring.liquibase.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BusinessRepositoryAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class BusinessRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private BusinessRepository repository;

    @Test
    void tenantBDoesNotFindTenantAProfile() {
        repository.save(Business.create(101L, "tenant-a", "Tenant A", null, null, null, null, null, null));

        assertThat(repository.findByTenantId(101L)).isPresent();
        assertThat(repository.findByTenantId(202L)).isEmpty();
    }
}
