package com.miaupy.notification.application;

import com.miaupy.scheduling.domain.Appointment;
import com.miaupy.scheduling.domain.AppointmentRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentReminderScheduler {
  private final AppointmentRepository appointments;
  private final NotificationComposer composer;
  private final Duration lead;
  private final Duration window;

  public AppointmentReminderScheduler(
      AppointmentRepository appointments,
      NotificationComposer composer,
      @Value("${miaupy.notification.appointment-reminder-lead:PT24H}") Duration lead,
      @Value("${miaupy.notification.appointment-reminder-window:PT5M}") Duration window) {
    this.appointments = appointments;
    this.composer = composer;
    this.lead = lead;
    this.window = window;
  }

  @Scheduled(fixedDelayString = "${miaupy.notification.reminder-scan-delay:PT5M}")
  @Transactional
  public void createReminders() {
    Instant start = Instant.now().plus(lead);
    Instant end = start.plus(window);
    for (Appointment appointment : appointments.findConfirmedStartingBetween(start, end)) {
      String source = "appointment-reminder:" + appointment.id() + ":" + appointment.startAt();
      UUID sourceId = UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
      composer.appointmentReminder(sourceId, appointment);
    }
  }
}
