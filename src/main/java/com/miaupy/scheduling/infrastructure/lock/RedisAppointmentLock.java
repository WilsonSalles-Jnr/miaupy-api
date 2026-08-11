package com.miaupy.scheduling.infrastructure.lock;

import com.miaupy.scheduling.application.AppointmentLock;
import com.miaupy.scheduling.domain.AppointmentConflictException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
class RedisAppointmentLock implements AppointmentLock {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisAppointmentLock.class);
  private static final Duration TTL = Duration.ofSeconds(15);
  private static final DefaultRedisScript<Long> RELEASE =
      new DefaultRedisScript<>(
          "if redis.call('get', KEYS[1]) == ARGV[1] then "
              + "return redis.call('del', KEYS[1]) else return 0 end",
          Long.class);
  private final StringRedisTemplate redis;

  RedisAppointmentLock(StringRedisTemplate redis) {
    this.redis = redis;
  }

  @Override
  public <T> T execute(
      Long tenantId, String scheduleResource, Instant startAt, Supplier<T> protectedAction) {
    String key =
        "appointment:lock:%d:%s:%d".formatted(tenantId, scheduleResource, startAt.toEpochMilli());
    String token = UUID.randomUUID().toString();
    boolean acquired = false;
    try {
      acquired = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, token, TTL));
      if (!acquired) {
        throw new AppointmentConflictException();
      }
    } catch (AppointmentConflictException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      LOGGER.warn(
          "Redis appointment lock unavailable; PostgreSQL constraint remains authoritative");
    }

    try {
      return protectedAction.get();
    } finally {
      if (acquired) {
        try {
          redis.execute(RELEASE, List.of(key), token);
        } catch (RuntimeException exception) {
          LOGGER.warn("Unable to release appointment lock; its TTL is 15 seconds");
        }
      }
    }
  }
}
