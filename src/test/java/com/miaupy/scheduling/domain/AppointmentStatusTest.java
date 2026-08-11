package com.miaupy.scheduling.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppointmentStatusTest {
  @Test
  void followsCentralizedLifecycle() {
    Appointment appointment = appointment(AppointmentStatus.REQUESTED);

    Appointment completed =
        appointment
            .transitionTo(AppointmentStatus.CONFIRMED)
            .transitionTo(AppointmentStatus.IN_PROGRESS)
            .transitionTo(AppointmentStatus.COMPLETED);

    assertThat(completed.status()).isEqualTo(AppointmentStatus.COMPLETED);
    assertThat(completed.status().occupiesSchedule()).isFalse();
  }

  @Test
  void rejectsInvalidTransition() {
    assertThatThrownBy(
            () ->
                appointment(AppointmentStatus.REQUESTED).transitionTo(AppointmentStatus.COMPLETED))
        .isInstanceOf(InvalidAppointmentTransitionException.class);
  }

  @Test
  void cancelledAppointmentDoesNotOccupySchedule() {
    Appointment cancelled =
        appointment(AppointmentStatus.REQUESTED).transitionTo(AppointmentStatus.CANCELLED);

    assertThat(cancelled.status().occupiesSchedule()).isFalse();
  }

  private Appointment appointment(AppointmentStatus status) {
    Instant start = Instant.now().plusSeconds(3600);
    Appointment value =
        Appointment.create(
            101L,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            AppointmentOrigin.CUSTOMER,
            start,
            start.plusSeconds(1800),
            AppointmentStatus.REQUESTED,
            null);
    return status == AppointmentStatus.CONFIRMED
        ? value.transitionTo(AppointmentStatus.CONFIRMED)
        : value;
  }
}
