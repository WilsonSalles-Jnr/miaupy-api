package com.miaupy.scheduling.domain;

public class AppointmentConflictException extends RuntimeException {
  public AppointmentConflictException() {
    super("The selected time interval is no longer available");
  }
}
