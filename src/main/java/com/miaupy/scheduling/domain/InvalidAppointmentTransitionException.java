package com.miaupy.scheduling.domain;

public class InvalidAppointmentTransitionException extends RuntimeException {
  public InvalidAppointmentTransitionException(
      AppointmentStatus current, AppointmentStatus target) {
    super("Appointment cannot transition from " + current + " to " + target);
  }
}
