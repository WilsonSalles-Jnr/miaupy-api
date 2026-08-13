package com.miaupy.clinical.infrastructure.persistence;

import com.miaupy.clinical.domain.ClinicalAttachment;
import com.miaupy.clinical.domain.ClinicalHistoryEvent;
import com.miaupy.clinical.domain.ClinicalRepository;
import com.miaupy.clinical.domain.Consultation;
import com.miaupy.clinical.domain.MedicalRecord;
import com.miaupy.clinical.domain.Prescription;
import com.miaupy.clinical.domain.Vaccination;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
class JdbcClinicalRepository implements ClinicalRepository {
  private final JdbcTemplate jdbc;

  JdbcClinicalRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<MedicalRecord> findMedicalRecord(UUID petId, Long tenantId) {
    return jdbc
        .query(
            "SELECT * FROM clinical.medical_record WHERE tenant_pet_id=? AND tenant_id=?",
            medicalRecordMapper(),
            petId,
            tenantId)
        .stream()
        .findFirst();
  }

  public MedicalRecord save(MedicalRecord record) {
    if (record.version() == null) {
      jdbc.update(
          "INSERT INTO clinical.medical_record(id,tenant_id,tenant_pet_id,allergies,chronic_conditions,current_medications,notes,created_by,updated_by,created_at,updated_at,version) VALUES (?,?,?,?,?,?,?,?,?,?,?,0)",
          record.id(),
          record.tenantId(),
          record.tenantPetId(),
          record.allergies(),
          record.chronicConditions(),
          record.currentMedications(),
          record.notes(),
          record.createdBy(),
          record.updatedBy(),
          record.createdAt(),
          record.updatedAt());
    } else {
      int changed =
          jdbc.update(
              "UPDATE clinical.medical_record SET allergies=?,chronic_conditions=?,current_medications=?,notes=?,updated_by=?,updated_at=?,version=version+1 WHERE id=? AND tenant_id=? AND version=?",
              record.allergies(),
              record.chronicConditions(),
              record.currentMedications(),
              record.notes(),
              record.updatedBy(),
              record.updatedAt(),
              record.id(),
              record.tenantId(),
              record.version());
      if (changed == 0) throw new OptimisticLockingFailureException("Medical record was modified");
    }
    return findMedicalRecord(record.tenantPetId(), record.tenantId()).orElseThrow();
  }

  public Consultation save(Consultation value) {
    jdbc.update(
        "INSERT INTO clinical.consultation(id,tenant_id,tenant_pet_id,appointment_id,occurred_at,reason,anamnesis,clinical_findings,diagnosis,treatment_plan,weight,temperature,veterinarian_subject,created_at,version) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)",
        value.id(),
        value.tenantId(),
        value.tenantPetId(),
        value.appointmentId(),
        value.occurredAt(),
        value.reason(),
        value.anamnesis(),
        value.clinicalFindings(),
        value.diagnosis(),
        value.treatmentPlan(),
        value.weight(),
        value.temperature(),
        value.veterinarianSubject(),
        value.createdAt());
    return value;
  }

  public Optional<Consultation> findConsultation(UUID id, UUID petId, Long tenantId) {
    return jdbc
        .query(
            "SELECT * FROM clinical.consultation WHERE id=? AND tenant_pet_id=? AND tenant_id=?",
            consultationMapper(),
            id,
            petId,
            tenantId)
        .stream()
        .findFirst();
  }

  public Vaccination save(Vaccination value) {
    jdbc.update(
        "INSERT INTO clinical.vaccination(id,tenant_id,tenant_pet_id,vaccine_name,manufacturer,batch_number,administered_on,next_due_on,veterinarian_subject,notes,created_at,version) VALUES (?,?,?,?,?,?,?,?,?,?,?,0)",
        value.id(),
        value.tenantId(),
        value.tenantPetId(),
        value.vaccineName(),
        value.manufacturer(),
        value.batchNumber(),
        value.administeredOn(),
        value.nextDueOn(),
        value.veterinarianSubject(),
        value.notes(),
        value.createdAt());
    return value;
  }

  public Prescription save(Prescription value) {
    jdbc.update(
        "INSERT INTO clinical.prescription(id,tenant_id,tenant_pet_id,consultation_id,medication,dosage,frequency,duration,instructions,issued_at,valid_until,veterinarian_subject,created_at,version) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,0)",
        value.id(),
        value.tenantId(),
        value.tenantPetId(),
        value.consultationId(),
        value.medication(),
        value.dosage(),
        value.frequency(),
        value.duration(),
        value.instructions(),
        value.issuedAt(),
        value.validUntil(),
        value.veterinarianSubject(),
        value.createdAt());
    return value;
  }

  public ClinicalAttachment save(ClinicalAttachment value) {
    jdbc.update(
        "INSERT INTO clinical.attachment(id,tenant_id,tenant_pet_id,consultation_id,original_filename,content_type,size_bytes,sha256,content,uploaded_by,active,created_at,version) VALUES (?,?,?,?,?,?,?,?,?,?,true,?,0)",
        value.id(),
        value.tenantId(),
        value.tenantPetId(),
        value.consultationId(),
        value.originalFilename(),
        value.contentType(),
        value.sizeBytes(),
        value.sha256(),
        value.content(),
        value.uploadedBy(),
        value.createdAt());
    return value;
  }

  public Optional<ClinicalAttachment> findAttachment(UUID id, UUID petId, Long tenantId) {
    return jdbc
        .query(
            "SELECT * FROM clinical.attachment WHERE id=? AND tenant_pet_id=? AND tenant_id=? AND active=true",
            attachmentMapper(),
            id,
            petId,
            tenantId)
        .stream()
        .findFirst();
  }

  public Page<ClinicalHistoryEvent> findHistory(UUID petId, Long tenantId, Pageable pageable) {
    Long total =
        jdbc.queryForObject(
            "SELECT count(*) FROM clinical.history_event WHERE tenant_pet_id=? AND tenant_id=?",
            Long.class,
            petId,
            tenantId);
    List<ClinicalHistoryEvent> values =
        jdbc.query(
            "SELECT * FROM clinical.history_event WHERE tenant_pet_id=? AND tenant_id=? ORDER BY occurred_at DESC LIMIT ? OFFSET ?",
            historyMapper(),
            petId,
            tenantId,
            pageable.getPageSize(),
            pageable.getOffset());
    return new PageImpl<>(values, pageable, total == null ? 0 : total);
  }

  public void appendHistory(ClinicalHistoryEvent value) {
    jdbc.update(
        "INSERT INTO clinical.history_event(id,tenant_id,tenant_pet_id,event_type,resource_id,summary,occurred_at,recorded_by,created_at) VALUES (?,?,?,?,?,?,?,?,?)",
        value.id(),
        value.tenantId(),
        value.tenantPetId(),
        value.eventType(),
        value.resourceId(),
        value.summary(),
        value.occurredAt(),
        value.recordedBy(),
        value.createdAt());
  }

  private RowMapper<MedicalRecord> medicalRecordMapper() {
    return (rs, row) ->
        new MedicalRecord(
            uuid(rs, "id"),
            rs.getLong("tenant_id"),
            uuid(rs, "tenant_pet_id"),
            rs.getString("allergies"),
            rs.getString("chronic_conditions"),
            rs.getString("current_medications"),
            rs.getString("notes"),
            rs.getString("created_by"),
            rs.getString("updated_by"),
            instant(rs, "created_at"),
            instant(rs, "updated_at"),
            rs.getLong("version"));
  }

  private RowMapper<Consultation> consultationMapper() {
    return (rs, row) ->
        new Consultation(
            uuid(rs, "id"),
            rs.getLong("tenant_id"),
            uuid(rs, "tenant_pet_id"),
            nullableUuid(rs, "appointment_id"),
            instant(rs, "occurred_at"),
            rs.getString("reason"),
            rs.getString("anamnesis"),
            rs.getString("clinical_findings"),
            rs.getString("diagnosis"),
            rs.getString("treatment_plan"),
            rs.getBigDecimal("weight"),
            rs.getBigDecimal("temperature"),
            rs.getString("veterinarian_subject"),
            instant(rs, "created_at"),
            rs.getLong("version"));
  }

  private RowMapper<ClinicalAttachment> attachmentMapper() {
    return (rs, row) ->
        new ClinicalAttachment(
            uuid(rs, "id"),
            rs.getLong("tenant_id"),
            uuid(rs, "tenant_pet_id"),
            nullableUuid(rs, "consultation_id"),
            rs.getString("original_filename"),
            rs.getString("content_type"),
            rs.getLong("size_bytes"),
            rs.getString("sha256"),
            rs.getBytes("content"),
            rs.getString("uploaded_by"),
            rs.getBoolean("active"),
            instantNullable(rs, "deleted_at"),
            instant(rs, "created_at"),
            rs.getLong("version"));
  }

  private RowMapper<ClinicalHistoryEvent> historyMapper() {
    return (rs, row) ->
        new ClinicalHistoryEvent(
            uuid(rs, "id"),
            rs.getLong("tenant_id"),
            uuid(rs, "tenant_pet_id"),
            rs.getString("event_type"),
            uuid(rs, "resource_id"),
            rs.getString("summary"),
            instant(rs, "occurred_at"),
            rs.getString("recorded_by"),
            instant(rs, "created_at"));
  }

  private UUID uuid(ResultSet rs, String column) throws SQLException {
    return rs.getObject(column, UUID.class);
  }

  private UUID nullableUuid(ResultSet rs, String column) throws SQLException {
    return rs.getObject(column, UUID.class);
  }

  private Instant instant(ResultSet rs, String column) throws SQLException {
    return rs.getTimestamp(column).toInstant();
  }

  private Instant instantNullable(ResultSet rs, String column) throws SQLException {
    var value = rs.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }
}
