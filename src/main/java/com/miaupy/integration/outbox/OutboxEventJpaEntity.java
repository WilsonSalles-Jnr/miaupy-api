package com.miaupy.integration.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "domain_event_outbox", schema = "integration")
class OutboxEventJpaEntity {
  @Id UUID id;

  @Column(name = "aggregate_type", nullable = false)
  String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  UUID aggregateId;

  @Column(name = "event_type", nullable = false)
  String eventType;

  @Column(name = "event_version", nullable = false)
  int eventVersion;

  @Column(name = "occurred_at", nullable = false)
  Instant occurredAt;

  @Column(name = "tenant_id")
  Long tenantId;

  @Column(name = "actor_type", nullable = false)
  String actorType;

  @Column(name = "actor_id", nullable = false)
  String actorId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  String payload;

  @Column(nullable = false)
  String status;

  @Column(name = "created_at", nullable = false)
  Instant createdAt;

  protected OutboxEventJpaEntity() {}

  OutboxEventJpaEntity(OutboxEvent event) {
    id = event.eventId();
    aggregateType = event.aggregateType();
    aggregateId = event.aggregateId();
    eventType = event.eventType();
    eventVersion = event.eventVersion();
    occurredAt = event.occurredAt();
    tenantId = event.tenantId();
    actorType = event.actorType();
    actorId = event.actorId();
    payload = event.payload();
    status = "PENDING";
    createdAt = Instant.now();
  }
}
