package com.miaupy.business.domain;
import java.util.Optional;
public interface BusinessConfigurationRepository {
    Optional<BusinessSettings> findSettingsByTenantId(Long tenantId); BusinessSettings saveSettings(BusinessSettings settings);
    Optional<BusinessAddress> findAddressByTenantId(Long tenantId); BusinessAddress saveAddress(BusinessAddress address);
}
