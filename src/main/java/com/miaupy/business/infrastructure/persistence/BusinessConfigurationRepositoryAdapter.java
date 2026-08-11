package com.miaupy.business.infrastructure.persistence;

import com.miaupy.business.domain.*;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

interface SettingsJpaRepository extends JpaRepository<BusinessSettingsJpaEntity, Long> {}

interface AddressJpaRepository extends JpaRepository<BusinessAddressJpaEntity, UUID> {
  Optional<BusinessAddressJpaEntity> findByTenantId(Long tenantId);
}

@Repository
class BusinessConfigurationRepositoryAdapter implements BusinessConfigurationRepository {
  private final SettingsJpaRepository settings;
  private final AddressJpaRepository addresses;

  BusinessConfigurationRepositoryAdapter(SettingsJpaRepository s, AddressJpaRepository a) {
    settings = s;
    addresses = a;
  }

  public Optional<BusinessSettings> findSettingsByTenantId(Long t) {
    return settings.findById(t).map(this::map);
  }

  public BusinessSettings saveSettings(BusinessSettings s) {
    return map(settings.save(new BusinessSettingsJpaEntity(s)));
  }

  public Optional<BusinessAddress> findAddressByTenantId(Long t) {
    return addresses.findByTenantId(t).map(this::map);
  }

  public BusinessAddress saveAddress(BusinessAddress a) {
    return map(addresses.save(new BusinessAddressJpaEntity(a)));
  }

  private BusinessSettings map(BusinessSettingsJpaEntity e) {
    return new BusinessSettings(
        e.tenantId, e.mode, e.timezone, e.currency, e.booking, e.sales, e.createdAt, e.updatedAt);
  }

  private BusinessAddress map(BusinessAddressJpaEntity e) {
    return new BusinessAddress(
        e.id,
        e.tenantId,
        e.businessId,
        e.street,
        e.number,
        e.district,
        e.city,
        e.state,
        e.postalCode,
        e.latitude,
        e.longitude,
        e.createdAt,
        e.updatedAt);
  }
}
