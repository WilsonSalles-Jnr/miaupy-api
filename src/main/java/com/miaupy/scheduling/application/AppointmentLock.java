package com.miaupy.scheduling.application;

import java.time.Instant;
import java.util.function.Supplier;

public interface AppointmentLock {
  <T> T execute(
      Long tenantId, String scheduleResource, Instant startAt, Supplier<T> protectedAction);
}
