package com.miaupy.scheduling.application;

import com.miaupy.business.domain.AppointmentApprovalMode;
import com.miaupy.business.domain.Business;
import com.miaupy.business.domain.BusinessConfigurationRepository;
import com.miaupy.business.domain.BusinessRepository;
import com.miaupy.business.domain.BusinessSettings;
import com.miaupy.consumer.application.ConsumerProfileUseCase;
import com.miaupy.consumer.domain.ConsumerProfile;
import com.miaupy.customer.domain.TenantCustomer;
import com.miaupy.pet.domain.TenantPet;
import com.miaupy.scheduling.domain.Appointment;
import com.miaupy.scheduling.domain.AppointmentConflictException;
import com.miaupy.scheduling.domain.AppointmentOrigin;
import com.miaupy.scheduling.domain.AppointmentRepository;
import com.miaupy.scheduling.domain.AppointmentStatus;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.tenant.TenantContext;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentUseCase {
  private final TenantContext tenantContext;
  private final BusinessRepository businesses;
  private final BusinessConfigurationRepository configurations;
  private final ConsumerProfileUseCase profiles;
  private final AppointmentRepository appointments;
  private final AppointmentTransaction transaction;
  private final AppointmentLock lock;
  private final AvailabilityUseCase availability;
  private final ConsumerStoreLinkUseCase storeLinks;

  public AppointmentUseCase(
      TenantContext tenantContext,
      BusinessRepository businesses,
      BusinessConfigurationRepository configurations,
      ConsumerProfileUseCase profiles,
      AppointmentRepository appointments,
      AppointmentTransaction transaction,
      AppointmentLock lock,
      AvailabilityUseCase availability,
      ConsumerStoreLinkUseCase storeLinks) {
    this.tenantContext = tenantContext;
    this.businesses = businesses;
    this.configurations = configurations;
    this.profiles = profiles;
    this.appointments = appointments;
    this.transaction = transaction;
    this.lock = lock;
    this.availability = availability;
    this.storeLinks = storeLinks;
  }

  public Appointment createBusiness(Command command) {
    Long tenantId = tenantContext.getRequiredTenantId();
    return create(
        new AppointmentTransaction.CreateCommand(
            tenantId,
            command.customerId(),
            command.petId(),
            command.serviceId(),
            command.employeeId(),
            AppointmentOrigin.BUSINESS,
            AppointmentStatus.CONFIRMED,
            command.startAt(),
            command.notes()));
  }

  public Appointment createConsumer(ConsumerCommand command) {
    ConsumerProfile profile = consumerProfile();
    Business business =
        businesses
            .findPublicBySlug(command.storeSlug().strip().toLowerCase())
            .orElseThrow(() -> new ResourceNotFoundException("Public store not found"));
    ConsumerStoreLinkUseCase.LinkedCustomerPet link =
        storeLinks.link(profile, command.consumerPetId(), business.tenantId());
    TenantCustomer customer = link.customer();
    TenantPet pet = link.pet();
    BusinessSettings settings =
        configurations
            .findSettingsByTenantId(business.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("Business settings not found"));
    if (!settings.allowOnlineBooking()) {
      throw new IllegalArgumentException("Online booking is disabled for this store");
    }
    boolean available =
        availability
            .publicAvailability(
                command.storeSlug(),
                command.serviceId(),
                command.startAt().atZone(ZoneId.of(settings.timezone())).toLocalDate(),
                command.employeeId())
            .stream()
            .anyMatch(
                slot ->
                    slot.startAt().equals(command.startAt())
                        && Objects.equals(slot.employeeId(), command.employeeId()));
    if (!available) {
      throw new AppointmentConflictException();
    }
    AppointmentStatus initial =
        settings.appointmentApprovalMode() == AppointmentApprovalMode.AUTOMATIC
            ? AppointmentStatus.CONFIRMED
            : AppointmentStatus.REQUESTED;
    return create(
        new AppointmentTransaction.CreateCommand(
            business.tenantId(),
            customer.id(),
            pet.id(),
            command.serviceId(),
            command.employeeId(),
            AppointmentOrigin.CUSTOMER,
            initial,
            command.startAt(),
            command.notes()));
  }

  @Transactional(readOnly = true)
  public Page<Appointment> listBusiness(int page, int size) {
    return appointments.findAllByTenantId(tenantContext.getRequiredTenantId(), page(page, size));
  }

  @Transactional(readOnly = true)
  public Page<Appointment> listConsumer(int page, int size) {
    return appointments.findAllByConsumerProfileId(consumerProfile().id(), page(page, size));
  }

  public Appointment transitionBusiness(UUID id, AppointmentStatus target) {
    return transaction.transitionBusiness(id, tenantContext.getRequiredTenantId(), target);
  }

  public Appointment cancelConsumer(UUID id) {
    return transaction.cancelConsumer(id, consumerProfile().id());
  }

  private Appointment create(AppointmentTransaction.CreateCommand command) {
    String resource = Appointment.resource(command.employeeId(), command.serviceId());
    try {
      return lock.execute(
          command.tenantId(), resource, command.startAt(), () -> transaction.create(command));
    } catch (DataIntegrityViolationException exception) {
      throw new AppointmentConflictException();
    }
  }

  private ConsumerProfile consumerProfile() {
    return profiles.getMe();
  }

  private PageRequest page(int page, int size) {
    return PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
  }

  public record Command(
      UUID customerId,
      UUID petId,
      UUID serviceId,
      UUID employeeId,
      Instant startAt,
      String notes) {}

  public record ConsumerCommand(
      String storeSlug,
      UUID consumerPetId,
      UUID serviceId,
      UUID employeeId,
      Instant startAt,
      String notes) {}
}
