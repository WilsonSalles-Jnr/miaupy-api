package com.miaupy.business.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miaupy.business.domain.Business;
import com.miaupy.business.domain.BusinessAddress;
import com.miaupy.business.domain.BusinessConfigurationRepository;
import com.miaupy.business.domain.BusinessRepository;
import com.miaupy.business.domain.BusinessSettings;
import com.miaupy.shared.exception.ConflictException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreateBusinessUseCaseTest {

  @Test
  void derivesTenantFromContextAndStartsPrivate() {
    InMemoryBusinessRepository repository = new InMemoryBusinessRepository();
    CreateBusinessUseCase useCase = new CreateBusinessUseCase(() -> 101L, repository, repository);

    Business created = useCase.execute(command(" Minha-Loja "));

    assertThat(created.tenantId()).isEqualTo(101L);
    assertThat(created.slug()).isEqualTo("minha-loja");
    assertThat(created.publicVisible()).isFalse();
    assertThat(repository.findByTenantId(202L)).isEmpty();
  }

  @Test
  void preventsSecondProfileForSameTenant() {
    InMemoryBusinessRepository repository = new InMemoryBusinessRepository();
    CreateBusinessUseCase useCase = new CreateBusinessUseCase(() -> 101L, repository, repository);
    useCase.execute(command("loja-a"));

    assertThatThrownBy(() -> useCase.execute(command("loja-b")))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void preventsSlugReuseAcrossTenants() {
    InMemoryBusinessRepository repository = new InMemoryBusinessRepository();
    new CreateBusinessUseCase(() -> 101L, repository, repository).execute(command("loja"));

    assertThatThrownBy(
            () ->
                new CreateBusinessUseCase(() -> 202L, repository, repository)
                    .execute(command("loja")))
        .isInstanceOf(ConflictException.class);
  }

  private BusinessCommand command(String slug) {
    return new BusinessCommand(slug, "Miaupy", null, null, null, null, null, null, true);
  }

  private static final class InMemoryBusinessRepository
      implements BusinessRepository, BusinessConfigurationRepository {
    private final Map<Long, Business> businesses = new HashMap<>();
    private final Map<Long, BusinessSettings> settings = new HashMap<>();

    @Override
    public Business save(Business business) {
      businesses.put(business.tenantId(), business);
      return business;
    }

    @Override
    public Optional<Business> findByTenantId(Long tenantId) {
      return Optional.ofNullable(businesses.get(tenantId));
    }

    @Override
    public Optional<Business> findPublicBySlug(String slug) {
      return businesses.values().stream()
          .filter(
              business ->
                  business.slug().equals(slug) && business.active() && business.publicVisible())
          .findFirst();
    }

    @Override
    public boolean existsByTenantId(Long tenantId) {
      return businesses.containsKey(tenantId);
    }

    @Override
    public boolean existsBySlugAndDifferentTenant(String slug, Long tenantId) {
      return businesses.values().stream()
          .anyMatch(
              business -> business.slug().equals(slug) && !business.tenantId().equals(tenantId));
    }

    public Optional<BusinessSettings> findSettingsByTenantId(Long tenantId) {
      return Optional.ofNullable(settings.get(tenantId));
    }

    public BusinessSettings saveSettings(BusinessSettings value) {
      settings.put(value.tenantId(), value);
      return value;
    }

    public Optional<BusinessAddress> findAddressByTenantId(Long tenantId) {
      return Optional.empty();
    }

    public BusinessAddress saveAddress(BusinessAddress address) {
      return address;
    }
  }
}
