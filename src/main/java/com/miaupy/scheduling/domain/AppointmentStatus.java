package com.miaupy.scheduling.domain;

import java.util.Set;

public enum AppointmentStatus {
  REQUESTED,
  CONFIRMED,
  REJECTED,
  CANCELLED,
  IN_PROGRESS,
  COMPLETED,
  NO_SHOW;

  public boolean occupiesSchedule() {
    return this == REQUESTED || this == CONFIRMED || this == IN_PROGRESS;
  }

  public boolean canTransitionTo(AppointmentStatus target) {
    return switch (this) {
      case REQUESTED -> Set.of(CONFIRMED, REJECTED, CANCELLED).contains(target);
      case CONFIRMED -> Set.of(CANCELLED, IN_PROGRESS, NO_SHOW).contains(target);
      case IN_PROGRESS -> target == COMPLETED;
      case REJECTED, CANCELLED, COMPLETED, NO_SHOW -> false;
    };
  }
}
