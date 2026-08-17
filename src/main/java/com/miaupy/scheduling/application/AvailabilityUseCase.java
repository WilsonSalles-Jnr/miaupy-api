package com.miaupy.scheduling.application;

import com.miaupy.business.domain.Business;
import com.miaupy.business.domain.BusinessConfigurationRepository;
import com.miaupy.business.domain.BusinessRepository;
import com.miaupy.catalog.domain.OfferedService;
import com.miaupy.catalog.domain.OfferedServiceRepository;
import com.miaupy.employee.application.EmployeeDirectory;
import com.miaupy.scheduling.domain.Appointment;
import com.miaupy.scheduling.domain.AppointmentRepository;
import com.miaupy.scheduling.domain.AvailabilityRule;
import com.miaupy.scheduling.domain.AvailabilityRuleRepository;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.tenant.TenantContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityUseCase {
  private final TenantContext tenants;
  private final BusinessRepository businesses;
  private final BusinessConfigurationRepository configurations;
  private final OfferedServiceRepository services;
  private final AvailabilityRuleRepository rules;
  private final AppointmentRepository appointments;
  private final EmployeeDirectory employees;

  public AvailabilityUseCase(
      TenantContext tenants,
      BusinessRepository businesses,
      BusinessConfigurationRepository configurations,
      OfferedServiceRepository services,
      AvailabilityRuleRepository rules,
      AppointmentRepository appointments,
      EmployeeDirectory employees) {
    this.tenants = tenants;
    this.businesses = businesses;
    this.configurations = configurations;
    this.services = services;
    this.rules = rules;
    this.appointments = appointments;
    this.employees = employees;
  }

  @Transactional
  public AvailabilityRule createRule(
      UUID employeeId,
      java.time.DayOfWeek day,
      java.time.LocalTime start,
      java.time.LocalTime end) {
    Long tenantId = tenants.getRequiredTenantId();
    if (employeeId != null) {
      employees.requireActive(employeeId, tenantId);
    }
    return rules.save(AvailabilityRule.create(tenantId, employeeId, day, start, end));
  }

  @Transactional(readOnly = true)
  public List<AvailabilityRule> listRules() {
    return rules.findAllByTenantId(tenants.getRequiredTenantId());
  }

  @Transactional
  public void deleteRule(UUID id) {
    Long tenantId = tenants.getRequiredTenantId();
    AvailabilityRule rule =
        rules
            .findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Availability rule not found"));
    rules.save(rule.deactivate());
  }

  @Transactional(readOnly = true)
  public List<AvailableSlot> publicAvailability(
      String slug, UUID serviceId, LocalDate date, UUID employeeId) {
    Business business =
        businesses
            .findPublicBySlug(slug.strip().toLowerCase())
            .orElseThrow(() -> new ResourceNotFoundException("Public store not found"));
    var settings =
        configurations
            .findSettingsByTenantId(business.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("Business settings not found"));
    if (!settings.allowOnlineBooking()) {
      return List.of();
    }
    if (employeeId != null) {
      employees.requireActive(employeeId, business.tenantId());
    }
    OfferedService service =
        services
            .findPublishedByIdAndTenantId(serviceId, business.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("Public service not found"));
    ZoneId zone = ZoneId.of(settings.timezone());
    Instant dayStart = date.atStartOfDay(zone).toInstant();
    Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
    List<Appointment> occupied =
        appointments.findOccupiedBetween(business.tenantId(), dayStart, dayEnd);

    return rules.findActiveByTenantIdAndDay(business.tenantId(), date.getDayOfWeek()).stream()
        .filter(rule -> employeeId == null || employeeId.equals(rule.employeeId()))
        .flatMap(
            rule ->
                slots(rule, date, zone, service.id(), service.durationMinutes(), occupied).stream())
        .distinct()
        .sorted(Comparator.comparing(AvailableSlot::startAt))
        .toList();
  }

  private List<AvailableSlot> slots(
      AvailabilityRule rule,
      LocalDate date,
      ZoneId zone,
      UUID serviceId,
      int durationMinutes,
      List<Appointment> occupied) {
    java.util.ArrayList<AvailableSlot> result = new java.util.ArrayList<>();
    ZonedDateTime cursor = date.atTime(rule.startLocal()).atZone(zone);
    ZonedDateTime limit = date.atTime(rule.endLocal()).atZone(zone);
    String resource = Appointment.resource(rule.employeeId(), serviceId);
    while (!cursor.plusMinutes(durationMinutes).isAfter(limit)) {
      Instant start = cursor.toInstant();
      Instant end = cursor.plusMinutes(durationMinutes).toInstant();
      boolean conflict =
          occupied.stream()
              .anyMatch(
                  appointment ->
                      appointment.scheduleResource().equals(resource)
                          && appointment.startAt().isBefore(end)
                          && appointment.endAt().isAfter(start));
      if (start.isAfter(Instant.now()) && !conflict) {
        result.add(new AvailableSlot(rule.employeeId(), start, end));
      }
      cursor = cursor.plusMinutes(durationMinutes);
    }
    return result;
  }

  public record AvailableSlot(UUID employeeId, Instant startAt, Instant endAt) {}
}
