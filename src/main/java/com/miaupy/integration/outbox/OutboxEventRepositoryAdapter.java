package com.miaupy.integration.outbox;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

interface SpringDataOutboxEventRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {
  @Query(
      value =
          "SELECT * FROM integration.domain_event_outbox WHERE status = 'PENDING' "
              + "ORDER BY created_at LIMIT 50 FOR UPDATE SKIP LOCKED",
      nativeQuery = true)
  java.util.List<OutboxEventJpaEntity> lockPendingBatch();
}

@Repository
class OutboxEventRepositoryAdapter implements OutboxEventRepository {
  private final SpringDataOutboxEventRepository repository;

  OutboxEventRepositoryAdapter(SpringDataOutboxEventRepository repository) {
    this.repository = repository;
  }

  @Override
  public void save(OutboxEvent event) {
    repository.save(new OutboxEventJpaEntity(event));
  }
}
