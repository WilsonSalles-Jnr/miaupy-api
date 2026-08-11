package com.miaupy.integration.outbox;

public interface OutboxEventRepository {
  void save(OutboxEvent event);
}
