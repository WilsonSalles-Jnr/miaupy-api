package com.miaupy.consumer.infrastructure.persistence;

import com.miaupy.consumer.application.ConsumerProfileProvisioningLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class PostgresConsumerProfileProvisioningLock implements ConsumerProfileProvisioningLock {
  private final JdbcTemplate jdbcTemplate;

  PostgresConsumerProfileProvisioningLock(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void lock(String authSubject) {
    jdbcTemplate.queryForObject(
        "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))::text", String.class, authSubject);
  }
}
