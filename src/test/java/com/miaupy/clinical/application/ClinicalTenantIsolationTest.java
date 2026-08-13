package com.miaupy.clinical.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miaupy.clinical.domain.ClinicalRepository;
import com.miaupy.integration.outbox.OutboxWriter;
import com.miaupy.pet.domain.TenantPetRepository;
import com.miaupy.scheduling.domain.AppointmentRepository;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.security.ActorContext;
import com.miaupy.shared.tenant.TenantContext;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClinicalTenantIsolationTest {
  @Test
  void tenantBCannotReadMedicalRecordFromPetOfTenantA() {
    UUID petId = UUID.randomUUID();
    TenantContext tenants = mock(TenantContext.class);
    TenantPetRepository pets = mock(TenantPetRepository.class);
    ClinicalRepository clinical = mock(ClinicalRepository.class);
    when(tenants.getRequiredTenantId()).thenReturn(202L);
    when(pets.findByIdAndTenantId(petId, 202L)).thenReturn(Optional.empty());
    ClinicalUseCase useCase = useCase(tenants, pets, clinical, mock(ActorContext.class));

    assertThatThrownBy(() -> useCase.getMedicalRecord(petId))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(pets).findByIdAndTenantId(petId, 202L);
    verify(clinical, never()).findMedicalRecord(petId, 202L);
  }

  private ClinicalUseCase useCase(
      TenantContext tenants,
      TenantPetRepository pets,
      ClinicalRepository clinical,
      ActorContext actors) {
    return new ClinicalUseCase(
        tenants,
        actors,
        pets,
        mock(AppointmentRepository.class),
        clinical,
        mock(OutboxWriter.class));
  }
}
