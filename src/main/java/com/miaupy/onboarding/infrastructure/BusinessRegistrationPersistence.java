package com.miaupy.onboarding.infrastructure;

import com.miaupy.onboarding.domain.BusinessRegistration;
import com.miaupy.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

interface BusinessRegistrationJpaRepository
    extends JpaRepository<BusinessRegistrationJpaEntity, UUID> {
  Optional<BusinessRegistrationJpaEntity> findByIdempotencyKey(UUID idempotencyKey);
}

@Repository
public class BusinessRegistrationPersistence {
  private final TenantJpaRepository tenants;
  private final BusinessRegistrationJpaRepository registrations;

  BusinessRegistrationPersistence(
      TenantJpaRepository tenants, BusinessRegistrationJpaRepository registrations) {
    this.tenants = tenants;
    this.registrations = registrations;
  }

  public Long createTenant() {
    return tenants.save(new TenantJpaEntity("ACTIVE", Instant.now())).id;
  }

  public Optional<BusinessRegistration> findByIdempotencyKey(UUID key) {
    return registrations.findByIdempotencyKey(key).map(BusinessRegistrationJpaEntity::toDomain);
  }

  public BusinessRegistration getRequired(UUID id) {
    return registrations
        .findById(id)
        .map(BusinessRegistrationJpaEntity::toDomain)
        .orElseThrow(() -> new ResourceNotFoundException("Business registration not found"));
  }

  public BusinessRegistration save(BusinessRegistration registration) {
    return registrations.save(new BusinessRegistrationJpaEntity(registration)).toDomain();
  }
}
