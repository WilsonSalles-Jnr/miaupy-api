--liquibase formatted sql

--changeset miaupy:131-backfill-clinical-history-details
UPDATE clinical.history_event history
SET details = jsonb_strip_nulls(jsonb_build_object(
    'allergies', record.allergies,
    'chronicConditions', record.chronic_conditions,
    'currentMedications', record.current_medications,
    'notes', record.notes
))
FROM clinical.medical_record record
WHERE history.event_type = 'MEDICAL_RECORD_UPDATED'
  AND record.id = history.resource_id
  AND record.tenant_id = history.tenant_id
  AND record.tenant_pet_id = history.tenant_pet_id;

UPDATE clinical.history_event history
SET details = jsonb_strip_nulls(jsonb_build_object(
    'appointmentId', consultation.appointment_id,
    'occurredAt', consultation.occurred_at,
    'reason', consultation.reason,
    'anamnesis', consultation.anamnesis,
    'clinicalFindings', consultation.clinical_findings,
    'diagnosis', consultation.diagnosis,
    'treatmentPlan', consultation.treatment_plan,
    'weight', consultation.weight,
    'temperature', consultation.temperature
))
FROM clinical.consultation consultation
WHERE history.event_type = 'CONSULTATION_RECORDED'
  AND consultation.id = history.resource_id
  AND consultation.tenant_id = history.tenant_id
  AND consultation.tenant_pet_id = history.tenant_pet_id;

UPDATE clinical.history_event history
SET details = jsonb_strip_nulls(jsonb_build_object(
    'vaccineName', vaccination.vaccine_name,
    'manufacturer', vaccination.manufacturer,
    'batchNumber', vaccination.batch_number,
    'administeredOn', vaccination.administered_on,
    'nextDueOn', vaccination.next_due_on,
    'notes', vaccination.notes
))
FROM clinical.vaccination vaccination
WHERE history.event_type = 'VACCINATION_RECORDED'
  AND vaccination.id = history.resource_id
  AND vaccination.tenant_id = history.tenant_id
  AND vaccination.tenant_pet_id = history.tenant_pet_id;

UPDATE clinical.history_event history
SET details = jsonb_strip_nulls(jsonb_build_object(
    'consultationId', prescription.consultation_id,
    'medication', prescription.medication,
    'dosage', prescription.dosage,
    'frequency', prescription.frequency,
    'duration', prescription.duration,
    'instructions', prescription.instructions,
    'issuedAt', prescription.issued_at,
    'validUntil', prescription.valid_until
))
FROM clinical.prescription prescription
WHERE history.event_type = 'PRESCRIPTION_ISSUED'
  AND prescription.id = history.resource_id
  AND prescription.tenant_id = history.tenant_id
  AND prescription.tenant_pet_id = history.tenant_pet_id;

UPDATE clinical.history_event history
SET details = jsonb_strip_nulls(jsonb_build_object(
    'consultationId', attachment.consultation_id,
    'filename', attachment.original_filename,
    'contentType', attachment.content_type,
    'sizeBytes', attachment.size_bytes
))
FROM clinical.attachment attachment
WHERE history.event_type = 'ATTACHMENT_ADDED'
  AND attachment.id = history.resource_id
  AND attachment.tenant_id = history.tenant_id
  AND attachment.tenant_pet_id = history.tenant_pet_id;
