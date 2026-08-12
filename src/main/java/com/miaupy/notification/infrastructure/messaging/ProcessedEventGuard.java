package com.miaupy.notification.infrastructure.messaging;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class ProcessedEventGuard {
  private static final String CONSUMER = "notification-domain-events-v1";
  private final JdbcTemplate jdbc;

  ProcessedEventGuard(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  boolean acquire(UUID eventId) {
    return jdbc.update(
            "INSERT INTO integration.processed_event(consumer_name,event_id) VALUES (?,?) "
                + "ON CONFLICT DO NOTHING",
            CONSUMER,
            eventId)
        == 1;
  }
}
