package com.miaupy.business.infrastructure.persistence;

import com.miaupy.business.domain.*;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "business_settings", schema = "platform")
class BusinessSettingsJpaEntity {
  @Id
  @Column(name = "tenant_id")
  Long tenantId;

  @Enumerated(EnumType.STRING)
  @Column(name = "appointment_approval_mode", nullable = false, length = 30)
  AppointmentApprovalMode mode;

  @Column(nullable = false, length = 64)
  String timezone;

  @Column(nullable = false, length = 3)
  String currency;

  @Column(name = "allow_online_booking", nullable = false)
  boolean booking;

  @Column(name = "allow_online_sales", nullable = false)
  boolean sales;

  @Column(name = "created_at", nullable = false)
  Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  protected BusinessSettingsJpaEntity() {}

  BusinessSettingsJpaEntity(BusinessSettings s) {
    tenantId = s.tenantId();
    mode = s.appointmentApprovalMode();
    timezone = s.timezone();
    currency = s.currency();
    booking = s.allowOnlineBooking();
    sales = s.allowOnlineSales();
    createdAt = s.createdAt();
    updatedAt = s.updatedAt();
  }
}
