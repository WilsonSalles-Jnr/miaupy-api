package com.miaupy.scheduling.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = "spring.liquibase.enabled=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AppointmentConcurrencyIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired SpringDataAppointmentRepository springDataAppointments;

  @Test
  void listsConsumerAppointmentsUsingPhysicalCreatedAtColumn() {
    long tenant = 502L;
    UUID business = UUID.randomUUID();
    UUID profile = UUID.randomUUID();
    UUID customer = UUID.randomUUID();
    UUID pet = UUID.randomUUID();
    UUID service = UUID.randomUUID();
    UUID appointment = UUID.randomUUID();
    Instant now = Instant.now();

    jdbc.update(
        "INSERT INTO platform.business(id,tenant_id,slug,name,active,public_visible,created_at,updated_at,version) VALUES (?,?,?,?,true,false,?,?,0)",
        business,
        tenant,
        "consumer-appointments-store",
        "Store",
        Timestamp.from(now),
        Timestamp.from(now));
    jdbc.update(
        "INSERT INTO consumer.consumer_profile(id,auth_subject,name,email,active,created_at,updated_at,version) VALUES (?,?,?,?,true,?,?,0)",
        profile,
        "consumer-appointments-subject",
        "Consumer",
        "consumer-appointments@example.com",
        Timestamp.from(now),
        Timestamp.from(now));
    jdbc.update(
        "INSERT INTO crm.tenant_customer(id,tenant_id,consumer_profile_id,name,active,created_at,updated_at,version) VALUES (?,?,?,?,true,?,?,0)",
        customer,
        tenant,
        profile,
        "Consumer",
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
    jdbc.update(
        "INSERT INTO catalog.service(id,tenant_id,name,duration_minutes,price,active,published,requires_approval,created_at,updated_at,version) VALUES (?,?,?,30,?,true,true,true,?,?,0)",
        service,
        tenant,
        "Consultation",
        new BigDecimal("100.00"),
        Timestamp.from(now),
        Timestamp.from(now));
    jdbc.update(
        "INSERT INTO scheduling.appointment(id,tenant_id,tenant_customer_id,tenant_pet_id,service_id,schedule_resource,requested_by,start_at,end_at,status,created_at,updated_at,version) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,0)",
        appointment,
        tenant,
        customer,
        pet,
        service,
        "service:" + service,
        "CUSTOMER",
        Timestamp.from(now.plusSeconds(3600)),
        Timestamp.from(now.plusSeconds(5400)),
        "REQUESTED",
        Timestamp.from(now),
        Timestamp.from(now));

    var repository = new AppointmentRepositoryAdapter(springDataAppointments);
    var page =
        repository.findAllByConsumerProfileId(
            profile,
            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

    assertThat(page.getContent()).extracting(item -> item.id()).containsExactly(appointment);
  }

  @Test
  void postgresAllowsOnlyOneConcurrentAppointmentForOverlappingResource() throws Exception {
    long tenant = 501L;
    UUID business = UUID.randomUUID();
    UUID customer = UUID.randomUUID();
    UUID pet = UUID.randomUUID();
    UUID service = UUID.randomUUID();
    UUID employee = UUID.randomUUID();
    Instant now = Instant.now();
    jdbc.update(
        "INSERT INTO platform.business(id,tenant_id,slug,name,active,public_visible,created_at,updated_at,version) VALUES (?,?,?,?,true,false,?,?,0)",
        business,
        tenant,
        "concurrency-store",
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
    jdbc.update(
        "INSERT INTO catalog.service(id,tenant_id,name,duration_minutes,price,active,published,requires_approval,created_at,updated_at,version) VALUES (?,?,?,30,?,true,true,true,?,?,0)",
        service,
        tenant,
        "Consultation",
        new BigDecimal("100.00"),
        Timestamp.from(now),
        Timestamp.from(now));

    Instant start = now.plusSeconds(3600);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch go = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      Future<Boolean> first =
          executor.submit(() -> insert(tenant, customer, pet, service, employee, start, ready, go));
      Future<Boolean> second =
          executor.submit(() -> insert(tenant, customer, pet, service, employee, start, ready, go));
      ready.await();
      go.countDown();

      assertThat(java.util.List.of(first.get(), second.get()))
          .containsExactlyInAnyOrder(true, false);
    }
  }

  private boolean insert(
      long tenant,
      UUID customer,
      UUID pet,
      UUID service,
      UUID employee,
      Instant start,
      CountDownLatch ready,
      CountDownLatch go)
      throws InterruptedException {
    ready.countDown();
    go.await();
    try {
      jdbc.update(
          "INSERT INTO scheduling.appointment(id,tenant_id,tenant_customer_id,tenant_pet_id,service_id,employee_id,schedule_resource,requested_by,start_at,end_at,status,created_at,updated_at,version) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,0)",
          UUID.randomUUID(),
          tenant,
          customer,
          pet,
          service,
          employee,
          "employee:" + employee,
          "BUSINESS",
          Timestamp.from(start),
          Timestamp.from(start.plusSeconds(1800)),
          "CONFIRMED",
          Timestamp.from(Instant.now()),
          Timestamp.from(Instant.now()));
      return true;
    } catch (org.springframework.dao.DataIntegrityViolationException exception) {
      return false;
    }
  }
}
