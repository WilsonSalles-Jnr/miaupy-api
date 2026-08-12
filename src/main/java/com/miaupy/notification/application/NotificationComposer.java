package com.miaupy.notification.application;

import com.miaupy.business.domain.Business;
import com.miaupy.business.domain.BusinessConfigurationRepository;
import com.miaupy.business.domain.BusinessRepository;
import com.miaupy.consumer.domain.ConsumerProfile;
import com.miaupy.consumer.domain.ConsumerProfileRepository;
import com.miaupy.customer.domain.TenantCustomer;
import com.miaupy.customer.domain.TenantCustomerRepository;
import com.miaupy.notification.domain.Notification;
import com.miaupy.notification.domain.NotificationRepository;
import com.miaupy.order.domain.CustomerOrder;
import com.miaupy.order.domain.CustomerOrderRepository;
import com.miaupy.scheduling.domain.Appointment;
import com.miaupy.scheduling.domain.AppointmentRepository;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NotificationComposer {
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private final NotificationRepository notifications;
  private final AppointmentRepository appointments;
  private final CustomerOrderRepository orders;
  private final TenantCustomerRepository customers;
  private final ConsumerProfileRepository consumers;
  private final BusinessRepository businesses;
  private final BusinessConfigurationRepository configurations;

  public NotificationComposer(
      NotificationRepository notifications,
      AppointmentRepository appointments,
      CustomerOrderRepository orders,
      TenantCustomerRepository customers,
      ConsumerProfileRepository consumers,
      BusinessRepository businesses,
      BusinessConfigurationRepository configurations) {
    this.notifications = notifications;
    this.appointments = appointments;
    this.orders = orders;
    this.customers = customers;
    this.consumers = consumers;
    this.businesses = businesses;
    this.configurations = configurations;
  }

  public void compose(UUID eventId, String eventType, Long tenantId, UUID aggregateId) {
    if (tenantId == null) return;
    switch (eventType) {
      case "appointment.requested" -> appointmentForBusiness(eventId, tenantId, aggregateId);
      case "appointment.confirmed",
              "appointment.rejected",
              "appointment.cancelled",
              "appointment.completed" ->
          appointmentForConsumer(eventId, eventType, tenantId, aggregateId);
      case "order.created" -> orderCreated(eventId, tenantId, aggregateId);
      case "order.processing", "order.ready", "order.cancelled", "order.completed" ->
          orderForConsumer(eventId, eventType, tenantId, aggregateId);
      default -> {
        // Events outside the allowlist never trigger external communication.
      }
    }
  }

  public void appointmentReminder(UUID sourceId, Appointment appointment) {
    TenantCustomer customer = customer(appointment);
    if (customer == null) return;
    if (customer.email() == null || customer.email().isBlank()) return;
    enqueue(
        sourceId,
        "appointment.reminder",
        appointment.tenantId(),
        customer.consumerProfileId(),
        customer.email(),
        "Lembrete de agendamento",
        "Você possui um agendamento em "
            + format(appointment.startAt(), appointment.tenantId())
            + ".",
        "appointment-reminder:" + appointment.id() + ":" + appointment.startAt());
  }

  private void appointmentForBusiness(UUID eventId, Long tenantId, UUID appointmentId) {
    Business business = businesses.findByTenantId(tenantId).orElse(null);
    if (business == null || business.email() == null || business.email().isBlank()) return;
    Appointment appointment =
        appointments.findByIdAndTenantId(appointmentId, tenantId).orElse(null);
    if (appointment == null) return;
    enqueue(
        eventId,
        "appointment.requested",
        tenantId,
        null,
        business.email(),
        "Nova solicitação de agendamento",
        "Uma nova solicitação foi recebida para " + format(appointment.startAt(), tenantId) + ".",
        eventId + ":EMAIL:business");
  }

  private void appointmentForConsumer(
      UUID eventId, String eventType, Long tenantId, UUID appointmentId) {
    Appointment appointment =
        appointments.findByIdAndTenantId(appointmentId, tenantId).orElse(null);
    if (appointment == null) return;
    TenantCustomer customer = customer(appointment);
    if (customer == null) return;
    if (customer.email() == null || customer.email().isBlank()) return;
    String state = eventType.substring("appointment.".length()).replace('-', ' ');
    enqueue(
        eventId,
        eventType,
        tenantId,
        customer.consumerProfileId(),
        customer.email(),
        "Atualização do agendamento",
        "Seu agendamento de "
            + format(appointment.startAt(), tenantId)
            + " foi atualizado para "
            + state
            + ".",
        eventId + ":EMAIL:consumer");
  }

  private void orderCreated(UUID eventId, Long tenantId, UUID orderId) {
    orderForConsumer(eventId, "order.created", tenantId, orderId);
    Business business = businesses.findByTenantId(tenantId).orElse(null);
    CustomerOrder order = orders.findByIdAndTenantId(orderId, tenantId).orElse(null);
    if (business == null || order == null || business.email() == null || business.email().isBlank())
      return;
    enqueue(
        eventId,
        "order.created",
        tenantId,
        null,
        business.email(),
        "Novo pedido recebido",
        "O pedido " + order.id() + " foi criado com total de " + order.total() + ".",
        eventId + ":EMAIL:business");
  }

  private void orderForConsumer(UUID eventId, String eventType, Long tenantId, UUID orderId) {
    CustomerOrder order = orders.findByIdAndTenantId(orderId, tenantId).orElse(null);
    if (order == null) return;
    ConsumerProfile consumer = consumers.findById(order.consumerProfileId()).orElse(null);
    if (consumer == null || consumer.email() == null || consumer.email().isBlank()) return;
    String state = eventType.substring("order.".length()).replace('-', ' ');
    enqueue(
        eventId,
        eventType,
        tenantId,
        consumer.id(),
        consumer.email(),
        "Atualização do pedido",
        "O pedido " + order.id() + " foi atualizado para " + state + ".",
        eventId + ":EMAIL:consumer");
  }

  private TenantCustomer customer(Appointment appointment) {
    return customers
        .findByIdAndTenantId(appointment.tenantCustomerId(), appointment.tenantId())
        .orElse(null);
  }

  private String format(java.time.Instant instant, Long tenantId) {
    ZoneId zone =
        configurations
            .findSettingsByTenantId(tenantId)
            .map(settings -> ZoneId.of(settings.timezone()))
            .orElse(ZoneId.of("UTC"));
    return DATE_TIME.withZone(zone).format(instant) + " (" + zone + ")";
  }

  private void enqueue(
      UUID sourceEventId,
      String type,
      Long tenantId,
      UUID consumerProfileId,
      String recipient,
      String subject,
      String body,
      String deduplicationKey) {
    if (notifications.existsByDeduplicationKey(deduplicationKey)) return;
    notifications.save(
        Notification.email(
            tenantId,
            consumerProfileId,
            sourceEventId,
            deduplicationKey,
            type,
            recipient,
            subject,
            body));
  }
}
