package com.miaupy.clinical.application;

import com.miaupy.clinical.domain.ClinicalAttachment;
import com.miaupy.clinical.domain.ClinicalHistoryEvent;
import com.miaupy.clinical.domain.ClinicalRepository;
import com.miaupy.clinical.domain.Consultation;
import com.miaupy.clinical.domain.MedicalRecord;
import com.miaupy.clinical.domain.Prescription;
import com.miaupy.clinical.domain.Vaccination;
import com.miaupy.integration.outbox.OutboxWriter;
import com.miaupy.pet.domain.TenantPetRepository;
import com.miaupy.scheduling.domain.AppointmentRepository;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.security.ActorContext;
import com.miaupy.shared.tenant.TenantContext;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClinicalUseCase {
  private static final long MAX_ATTACHMENT_SIZE = 10L * 1024 * 1024;
  private static final Set<String> CONTENT_TYPES =
      Set.of("application/pdf", "image/jpeg", "image/png");
  private final TenantContext tenants;
  private final ActorContext actors;
  private final TenantPetRepository pets;
  private final AppointmentRepository appointments;
  private final ClinicalRepository clinical;
  private final OutboxWriter outbox;

  public ClinicalUseCase(
      TenantContext tenants,
      ActorContext actors,
      TenantPetRepository pets,
      AppointmentRepository appointments,
      ClinicalRepository clinical,
      OutboxWriter outbox) {
    this.tenants = tenants;
    this.actors = actors;
    this.pets = pets;
    this.appointments = appointments;
    this.clinical = clinical;
    this.outbox = outbox;
  }

  @Transactional(readOnly = true)
  public MedicalRecord getMedicalRecord(UUID petId) {
    Long tenantId = tenantAndPet(petId);
    return clinical
        .findMedicalRecord(petId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Medical record not found"));
  }

  @Transactional
  public MedicalRecord updateMedicalRecord(UUID petId, MedicalRecordCommand command) {
    Long tenantId = tenantAndPet(petId);
    String actor = actor();
    Instant now = Instant.now();
    MedicalRecord record =
        clinical
            .findMedicalRecord(petId, tenantId)
            .map(
                current ->
                    new MedicalRecord(
                        current.id(),
                        tenantId,
                        petId,
                        command.allergies(),
                        command.chronicConditions(),
                        command.currentMedications(),
                        command.notes(),
                        current.createdBy(),
                        actor,
                        current.createdAt(),
                        now,
                        current.version()))
            .orElseGet(
                () ->
                    new MedicalRecord(
                        UUID.randomUUID(),
                        tenantId,
                        petId,
                        command.allergies(),
                        command.chronicConditions(),
                        command.currentMedications(),
                        command.notes(),
                        actor,
                        actor,
                        now,
                        now,
                        null));
    MedicalRecord saved = clinical.save(record);
    history(
        tenantId, petId, "MEDICAL_RECORD_UPDATED", saved.id(), "Prontuário atualizado", now, actor);
    append(saved.id(), tenantId, petId, "clinical.medical-record.updated");
    return saved;
  }

  @Transactional
  public Consultation createConsultation(UUID petId, ConsultationCommand command) {
    Long tenantId = tenantAndPet(petId);
    if (command.appointmentId() != null) {
      var appointment =
          appointments
              .findByIdAndTenantId(command.appointmentId(), tenantId)
              .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
      if (!appointment.tenantPetId().equals(petId)) {
        throw new ResourceNotFoundException("Appointment not found for pet");
      }
    }
    String actor = actor();
    Instant now = Instant.now();
    Consultation value =
        new Consultation(
            UUID.randomUUID(),
            tenantId,
            petId,
            command.appointmentId(),
            command.occurredAt(),
            command.reason(),
            command.anamnesis(),
            command.clinicalFindings(),
            command.diagnosis(),
            command.treatmentPlan(),
            command.weight(),
            command.temperature(),
            actor,
            now,
            null);
    Consultation saved = clinical.save(value);
    history(
        tenantId,
        petId,
        "CONSULTATION_RECORDED",
        saved.id(),
        "Consulta registrada",
        saved.occurredAt(),
        actor);
    append(saved.id(), tenantId, petId, "clinical.consultation.created");
    return saved;
  }

  @Transactional
  public Vaccination createVaccination(UUID petId, VaccinationCommand command) {
    Long tenantId = tenantAndPet(petId);
    if (command.nextDueOn() != null && command.nextDueOn().isBefore(command.administeredOn())) {
      throw new IllegalArgumentException("Next vaccine due date cannot precede administration");
    }
    String actor = actor();
    Instant now = Instant.now();
    Vaccination value =
        new Vaccination(
            UUID.randomUUID(),
            tenantId,
            petId,
            command.vaccineName(),
            command.manufacturer(),
            command.batchNumber(),
            command.administeredOn(),
            command.nextDueOn(),
            actor,
            command.notes(),
            now,
            null);
    Vaccination saved = clinical.save(value);
    history(
        tenantId, petId, "VACCINATION_RECORDED", saved.id(), "Vacinação registrada", now, actor);
    append(saved.id(), tenantId, petId, "clinical.vaccination.created");
    return saved;
  }

  @Transactional
  public Prescription createPrescription(UUID petId, PrescriptionCommand command) {
    Long tenantId = tenantAndPet(petId);
    if (command.consultationId() != null) {
      clinical
          .findConsultation(command.consultationId(), petId, tenantId)
          .orElseThrow(() -> new ResourceNotFoundException("Consultation not found"));
    }
    String actor = actor();
    Instant now = Instant.now();
    Prescription value =
        new Prescription(
            UUID.randomUUID(),
            tenantId,
            petId,
            command.consultationId(),
            command.medication(),
            command.dosage(),
            command.frequency(),
            command.duration(),
            command.instructions(),
            now,
            command.validUntil(),
            actor,
            now,
            null);
    Prescription saved = clinical.save(value);
    history(tenantId, petId, "PRESCRIPTION_ISSUED", saved.id(), "Receita emitida", now, actor);
    append(saved.id(), tenantId, petId, "clinical.prescription.created");
    return saved;
  }

  @Transactional
  public ClinicalAttachment uploadAttachment(
      UUID petId, UUID consultationId, String filename, String contentType, byte[] content) {
    Long tenantId = tenantAndPet(petId);
    if (consultationId != null) {
      clinical
          .findConsultation(consultationId, petId, tenantId)
          .orElseThrow(() -> new ResourceNotFoundException("Consultation not found"));
    }
    validateAttachment(contentType, content);
    String actor = actor();
    Instant now = Instant.now();
    ClinicalAttachment value =
        new ClinicalAttachment(
            UUID.randomUUID(),
            tenantId,
            petId,
            consultationId,
            safeFilename(filename),
            contentType,
            content.length,
            sha256(content),
            content,
            actor,
            true,
            null,
            now,
            null);
    ClinicalAttachment saved = clinical.save(value);
    history(
        tenantId, petId, "ATTACHMENT_ADDED", saved.id(), "Anexo clínico adicionado", now, actor);
    append(saved.id(), tenantId, petId, "clinical.attachment.created");
    return saved;
  }

  @Transactional(readOnly = true)
  public ClinicalAttachment getAttachment(UUID petId, UUID attachmentId) {
    Long tenantId = tenantAndPet(petId);
    return clinical
        .findAttachment(attachmentId, petId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Clinical attachment not found"));
  }

  @Transactional(readOnly = true)
  public Page<ClinicalHistoryEvent> history(UUID petId, int page, int size) {
    Long tenantId = tenantAndPet(petId);
    return clinical.findHistory(
        petId,
        tenantId,
        PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "occurredAt")));
  }

  private Long tenantAndPet(UUID petId) {
    Long tenantId = tenants.getRequiredTenantId();
    pets.findByIdAndTenantId(petId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
    return tenantId;
  }

  private String actor() {
    return actors.getRequiredActor().subject();
  }

  private void history(
      Long tenant,
      UUID pet,
      String type,
      UUID resource,
      String summary,
      Instant occurredAt,
      String actor) {
    clinical.appendHistory(
        new ClinicalHistoryEvent(
            UUID.randomUUID(),
            tenant,
            pet,
            type,
            resource,
            summary,
            occurredAt,
            actor,
            Instant.now()));
  }

  private void append(UUID aggregateId, Long tenantId, UUID petId, String eventType) {
    outbox.append(
        "ClinicalRecord",
        aggregateId,
        eventType,
        tenantId,
        Map.of("resourceId", aggregateId, "tenantPetId", petId));
  }

  private void validateAttachment(String type, byte[] content) {
    if (!CONTENT_TYPES.contains(type))
      throw new IllegalArgumentException("Unsupported attachment type");
    if (content == null || content.length == 0 || content.length > MAX_ATTACHMENT_SIZE) {
      throw new IllegalArgumentException("Attachment size must be between 1 byte and 10 MB");
    }
    boolean matches =
        switch (type) {
          case "application/pdf" ->
              startsWith(content, "%PDF-".getBytes(StandardCharsets.US_ASCII));
          case "image/jpeg" ->
              content.length >= 3
                  && (content[0] & 0xff) == 0xff
                  && (content[1] & 0xff) == 0xd8
                  && (content[2] & 0xff) == 0xff;
          case "image/png" ->
              startsWith(
                  content, new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
          default -> false;
        };
    if (!matches)
      throw new IllegalArgumentException("Attachment content does not match content type");
  }

  private boolean startsWith(byte[] content, byte[] signature) {
    if (content.length < signature.length) return false;
    for (int i = 0; i < signature.length; i++) if (content[i] != signature[i]) return false;
    return true;
  }

  private String safeFilename(String filename) {
    if (filename == null || filename.isBlank()) return "attachment";
    String value = filename.replace('\\', '/');
    value = value.substring(value.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\u0000]", "");
    if (value.isBlank()) return "attachment";
    return value.length() > 255 ? value.substring(value.length() - 255) : value;
  }

  private String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  public record MedicalRecordCommand(
      String allergies, String chronicConditions, String currentMedications, String notes) {}

  public record ConsultationCommand(
      UUID appointmentId,
      Instant occurredAt,
      String reason,
      String anamnesis,
      String clinicalFindings,
      String diagnosis,
      String treatmentPlan,
      BigDecimal weight,
      BigDecimal temperature) {}

  public record VaccinationCommand(
      String vaccineName,
      String manufacturer,
      String batchNumber,
      LocalDate administeredOn,
      LocalDate nextDueOn,
      String notes) {}

  public record PrescriptionCommand(
      UUID consultationId,
      String medication,
      String dosage,
      String frequency,
      String duration,
      String instructions,
      LocalDate validUntil) {}
}
