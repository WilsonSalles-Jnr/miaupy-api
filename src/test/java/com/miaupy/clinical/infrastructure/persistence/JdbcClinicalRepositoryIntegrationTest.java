package com.miaupy.clinical.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.miaupy.clinical.domain.ClinicalAttachment;
import com.miaupy.clinical.domain.ClinicalHistoryEvent;
import com.miaupy.clinical.domain.Consultation;
import com.miaupy.clinical.domain.MedicalRecord;
import com.miaupy.clinical.domain.Prescription;
import com.miaupy.clinical.domain.Vaccination;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = "spring.liquibase.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcClinicalRepository.class)
@Testcontainers(disabledWithoutDocker = true)
class JdbcClinicalRepositoryIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired JdbcClinicalRepository repository;
  @Autowired JdbcTemplate jdbc;

  @Test
  void persistsEveryClinicalInstantAsPostgresTimestampWithTimeZone() {
    long tenant = 801L;
    UUID pet = seedTenantPet(tenant);
    Instant now = Instant.parse("2026-08-13T04:00:00Z");
    String actor = "veterinarian-subject";

    MedicalRecord medicalRecord =
        repository.save(
            new MedicalRecord(
                UUID.randomUUID(),
                tenant,
                pet,
                "None",
                "None",
                "None",
                "Healthy",
                actor,
                actor,
                now,
                now,
                null));
    repository.save(
        new MedicalRecord(
            medicalRecord.id(),
            tenant,
            pet,
            "Pollen",
            medicalRecord.chronicConditions(),
            medicalRecord.currentMedications(),
            medicalRecord.notes(),
            medicalRecord.createdBy(),
            actor,
            medicalRecord.createdAt(),
            now.plusSeconds(60),
            medicalRecord.version()));

    Consultation consultation =
        repository.save(
            new Consultation(
                UUID.randomUUID(),
                tenant,
                pet,
                null,
                now,
                "Routine visit",
                null,
                null,
                null,
                null,
                null,
                null,
                actor,
                now,
                null));
    repository.save(
        new Vaccination(
            UUID.randomUUID(),
            tenant,
            pet,
            "Rabies",
            null,
            null,
            LocalDate.of(2026, 8, 13),
            null,
            actor,
            null,
            now,
            null));
    repository.save(
        new Prescription(
            UUID.randomUUID(),
            tenant,
            pet,
            consultation.id(),
            "Medication",
            "10 mg",
            "Once daily",
            "7 days",
            null,
            now,
            null,
            actor,
            now,
            null));
    repository.save(
        new ClinicalAttachment(
            UUID.randomUUID(),
            tenant,
            pet,
            consultation.id(),
            "exam.pdf",
            "application/pdf",
            5,
            "0".repeat(64),
            "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII),
            actor,
            true,
            null,
            now,
            null));
    repository.appendHistory(
        new ClinicalHistoryEvent(
            UUID.randomUUID(),
            tenant,
            pet,
            "CONSULTATION_RECORDED",
            consultation.id(),
            "Consultation recorded",
            now,
            actor,
            "Veterinarian Test",
            java.util.Map.of("reason", "Annual check-up"),
            now));

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM clinical.history_event WHERE tenant_id=?",
                Long.class,
                tenant))
        .isEqualTo(1L);
    ClinicalHistoryEvent history =
        repository
            .findHistory(pet, tenant, org.springframework.data.domain.PageRequest.of(0, 10))
            .getContent()
            .getFirst();
    assertThat(history.recordedByName()).isEqualTo("Veterinarian Test");
    assertThat(history.details()).containsEntry("reason", "Annual check-up");
  }

  private UUID seedTenantPet(long tenant) {
    UUID business = UUID.randomUUID();
    UUID customer = UUID.randomUUID();
    UUID pet = UUID.randomUUID();
    Instant now = Instant.now();
    jdbc.update(
        "INSERT INTO platform.business(id,tenant_id,slug,name,active,public_visible,created_at,updated_at,version) VALUES (?,?,?,?,true,false,?,?,0)",
        business,
        tenant,
        "clinical-jdbc-store",
        "Store",
        Timestamp.from(now),
        Timestamp.from(now));
    jdbc.update(
        "INSERT INTO crm.tenant_customer(id,tenant_id,name,active,created_at,updated_at,version) VALUES (?,?,?,true,?,?,0)",
        customer,
        tenant,
        "Customer",
        Timestamp.from(now),
        Timestamp.from(now));
    jdbc.update(
        "INSERT INTO pet.tenant_pet(id,tenant_id,tenant_customer_id,name,species,active,created_at,updated_at,version) VALUES (?,?,?,?,?,true,?,?,0)",
        pet,
        tenant,
        customer,
        "Pet",
        "DOG",
        Timestamp.from(now),
        Timestamp.from(now));
    return pet;
  }
}
