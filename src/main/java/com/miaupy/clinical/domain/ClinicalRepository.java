package com.miaupy.clinical.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClinicalRepository {
  Optional<MedicalRecord> findMedicalRecord(UUID petId, Long tenantId);

  MedicalRecord save(MedicalRecord record);

  Consultation save(Consultation consultation);

  Optional<Consultation> findConsultation(UUID id, UUID petId, Long tenantId);

  Vaccination save(Vaccination vaccination);

  Prescription save(Prescription prescription);

  ClinicalAttachment save(ClinicalAttachment attachment);

  Optional<ClinicalAttachment> findAttachment(UUID id, UUID petId, Long tenantId);

  Page<ClinicalHistoryEvent> findHistory(UUID petId, Long tenantId, Pageable pageable);

  void appendHistory(ClinicalHistoryEvent event);
}
