package com.miaupy.integration.outbox;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

interface SpringDataOutboxEventRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {}

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
