package com.miaupy.clinical.api;

import com.miaupy.clinical.application.ClinicalUseCase;
import com.miaupy.clinical.domain.ClinicalAttachment;
import com.miaupy.clinical.domain.ClinicalHistoryEvent;
import com.miaupy.clinical.domain.Consultation;
import com.miaupy.clinical.domain.MedicalRecord;
import com.miaupy.clinical.domain.Prescription;
import com.miaupy.clinical.domain.Vaccination;
import com.miaupy.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/business/pets/{petId}/clinical")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','VETERINARIAN')")
public class ClinicalController {
  private final ClinicalUseCase useCase;

  public ClinicalController(ClinicalUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping("/medical-record")
  @Operation(
      summary = "Consultar prontuário veterinário",
      description =
          "Retorna o prontuário interno somente quando o pet pertence ao tenant autenticado.")
  public MedicalRecordResponse medicalRecord(
      @Parameter(description = "UUID do pet interno no tenant autenticado.") @PathVariable
          UUID petId) {
    return MedicalRecordResponse.from(useCase.getMedicalRecord(petId));
  }

  @PutMapping("/medical-record")
  @Operation(
      summary = "Criar ou atualizar prontuário",
      description =
          "Mantém alergias, condições e medicamentos do pet dentro do tenant, com autoria e optimistic locking.")
  public MedicalRecordResponse medicalRecord(
      @Parameter(description = "UUID do pet interno no tenant autenticado.") @PathVariable
          UUID petId,
      @Valid @RequestBody MedicalRecordRequest request) {
    return MedicalRecordResponse.from(
        useCase.updateMedicalRecord(
            petId,
            new ClinicalUseCase.MedicalRecordCommand(
                request.allergies(),
                request.chronicConditions(),
                request.currentMedications(),
                request.notes())));
  }

  @PostMapping("/consultations")
  @Operation(
      summary = "Registrar consulta veterinária",
      description =
          "Registra dados clínicos imutáveis da consulta e valida o vínculo opcional com o agendamento do mesmo pet e tenant.")
  public ConsultationResponse consultation(
      @Parameter(description = "UUID do pet interno no tenant autenticado.") @PathVariable
          UUID petId,
      @Valid @RequestBody ConsultationRequest request) {
    return ConsultationResponse.from(
        useCase.createConsultation(
            petId,
            new ClinicalUseCase.ConsultationCommand(
                request.appointmentId(),
                request.occurredAt(),
                request.reason(),
                request.anamnesis(),
                request.clinicalFindings(),
                request.diagnosis(),
                request.treatmentPlan(),
                request.weight(),
                request.temperature())));
  }

  @PostMapping("/vaccinations")
  @Operation(
      summary = "Registrar vacinação",
      description =
          "Registra vacina, lote, data de administração e próxima dose no histórico clínico do tenant.")
  public VaccinationResponse vaccination(
      @Parameter(description = "UUID do pet interno no tenant autenticado.") @PathVariable
          UUID petId,
      @Valid @RequestBody VaccinationRequest request) {
    return VaccinationResponse.from(
        useCase.createVaccination(
            petId,
            new ClinicalUseCase.VaccinationCommand(
                request.vaccineName(),
                request.manufacturer(),
                request.batchNumber(),
                request.administeredOn(),
                request.nextDueOn(),
                request.notes())));
  }

  @PostMapping("/prescriptions")
  @Operation(
      summary = "Emitir receita veterinária",
      description =
          "Registra receita vinculada opcionalmente a uma consulta do mesmo pet e tenant, preservando o autor.")
  public PrescriptionResponse prescription(
      @Parameter(description = "UUID do pet interno no tenant autenticado.") @PathVariable
          UUID petId,
      @Valid @RequestBody PrescriptionRequest request) {
    return PrescriptionResponse.from(
        useCase.createPrescription(
            petId,
            new ClinicalUseCase.PrescriptionCommand(
                request.consultationId(),
                request.medication(),
                request.dosage(),
                request.frequency(),
                request.duration(),
                request.instructions(),
                request.validUntil())));
  }

  @PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Adicionar anexo clínico",
      description =
          "Aceita somente PDF, JPEG ou PNG de até 10 MB, valida a assinatura binária, calcula SHA-256 e mantém o arquivo isolado pelo tenant.")
  public AttachmentResponse attachment(
      @Parameter(description = "UUID do pet interno no tenant autenticado.") @PathVariable
          UUID petId,
      @Parameter(description = "UUID opcional da consulta do mesmo pet.")
          @RequestParam(required = false)
          UUID consultationId,
      @Parameter(description = "Arquivo PDF, JPEG ou PNG com tamanho máximo de 10 MB.")
          @RequestParam("file")
          MultipartFile file)
      throws java.io.IOException {
    return AttachmentResponse.from(
        useCase.uploadAttachment(
            petId,
            consultationId,
            file.getOriginalFilename(),
            file.getContentType(),
            file.getBytes()));
  }

  @GetMapping("/attachments/{attachmentId}/content")
  @Operation(
      summary = "Baixar anexo clínico",
      description =
          "Baixa o conteúdo somente quando anexo, pet e tenant autenticado pertencem ao mesmo contexto.")
  public ResponseEntity<byte[]> attachmentContent(
      @Parameter(description = "UUID do pet interno no tenant autenticado.") @PathVariable
          UUID petId,
      @Parameter(description = "UUID do anexo clínico pertencente ao pet.") @PathVariable
          UUID attachmentId) {
    ClinicalAttachment value = useCase.getAttachment(petId, attachmentId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(value.contentType()))
        .contentLength(value.sizeBytes())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(value.originalFilename()).build().toString())
        .header("X-Content-Type-Options", "nosniff")
        .body(value.content());
  }

  @GetMapping("/history")
  @Operation(
      summary = "Consultar histórico clínico",
      description =
          "Lista eventos clínicos imutáveis do pet, paginados e filtrados pelo tenant do JWT.")
  public PageResponse<HistoryResponse> history(
      @Parameter(description = "UUID do pet interno no tenant autenticado.") @PathVariable
          UUID petId,
      @Parameter(description = "Índice da página, iniciando em zero.")
          @RequestParam(defaultValue = "0")
          @jakarta.validation.constraints.PositiveOrZero
          int page,
      @Parameter(description = "Quantidade de elementos por página, entre 1 e 100.")
          @RequestParam(defaultValue = "20")
          @jakarta.validation.constraints.Min(1)
          @jakarta.validation.constraints.Max(100)
          int size) {
    return PageResponse.from(useCase.history(petId, page, size), HistoryResponse::from);
  }

  public record MedicalRecordRequest(
      @Size(max = 3000) @Schema(description = "Alergias conhecidas do pet.") String allergies,
      @Size(max = 3000) @Schema(description = "Condições crônicas diagnosticadas.")
          String chronicConditions,
      @Size(max = 3000) @Schema(description = "Medicamentos usados atualmente.")
          String currentMedications,
      @Size(max = 5000) @Schema(description = "Observações clínicas internas do tenant.")
          String notes) {}

  public record ConsultationRequest(
      @Schema(description = "UUID opcional do agendamento associado à consulta.")
          UUID appointmentId,
      @NotNull @PastOrPresent @Schema(description = "Instante em que a consulta ocorreu.")
          Instant occurredAt,
      @NotBlank @Size(max = 1000) @Schema(description = "Motivo principal da consulta.")
          String reason,
      @Size(max = 5000) @Schema(description = "Anamnese registrada pelo veterinário.")
          String anamnesis,
      @Size(max = 5000) @Schema(description = "Achados do exame clínico.") String clinicalFindings,
      @Size(max = 3000) @Schema(description = "Diagnóstico ou hipótese diagnóstica.")
          String diagnosis,
      @Size(max = 5000) @Schema(description = "Plano terapêutico e recomendações.")
          String treatmentPlan,
      @Positive @Schema(description = "Peso medido durante a consulta.") BigDecimal weight,
      @DecimalMin("20.0")
          @DecimalMax("50.0")
          @Schema(description = "Temperatura corporal em graus Celsius, entre 20 e 50.")
          BigDecimal temperature) {}

  public record VaccinationRequest(
      @NotBlank @Size(max = 200) @Schema(description = "Nome da vacina administrada.")
          String vaccineName,
      @Size(max = 200) @Schema(description = "Fabricante da vacina.") String manufacturer,
      @Size(max = 100) @Schema(description = "Número do lote da vacina.") String batchNumber,
      @NotNull @PastOrPresent @Schema(description = "Data de administração da vacina.")
          LocalDate administeredOn,
      @Schema(description = "Data prevista para a próxima dose.") LocalDate nextDueOn,
      @Size(max = 3000) @Schema(description = "Observações sobre a vacinação.") String notes) {}

  public record PrescriptionRequest(
      @Schema(description = "UUID opcional da consulta que originou a receita.")
          UUID consultationId,
      @NotBlank @Size(max = 300) @Schema(description = "Nome do medicamento prescrito.")
          String medication,
      @NotBlank @Size(max = 500) @Schema(description = "Dose prescrita.") String dosage,
      @NotBlank @Size(max = 500) @Schema(description = "Frequência de administração.")
          String frequency,
      @Size(max = 500) @Schema(description = "Duração prevista do tratamento.") String duration,
      @Size(max = 3000) @Schema(description = "Instruções adicionais ao responsável.")
          String instructions,
      @Schema(description = "Data final de validade da receita.") LocalDate validUntil) {}

  public record MedicalRecordResponse(
      UUID id,
      UUID tenantPetId,
      String allergies,
      String chronicConditions,
      String currentMedications,
      String notes,
      Instant updatedAt) {
    static MedicalRecordResponse from(MedicalRecord value) {
      return new MedicalRecordResponse(
          value.id(),
          value.tenantPetId(),
          value.allergies(),
          value.chronicConditions(),
          value.currentMedications(),
          value.notes(),
          value.updatedAt());
    }
  }

  public record ConsultationResponse(
      UUID id,
      UUID appointmentId,
      Instant occurredAt,
      String reason,
      String diagnosis,
      BigDecimal weight,
      BigDecimal temperature) {
    static ConsultationResponse from(Consultation value) {
      return new ConsultationResponse(
          value.id(),
          value.appointmentId(),
          value.occurredAt(),
          value.reason(),
          value.diagnosis(),
          value.weight(),
          value.temperature());
    }
  }

  public record VaccinationResponse(
      UUID id,
      String vaccineName,
      String manufacturer,
      String batchNumber,
      LocalDate administeredOn,
      LocalDate nextDueOn) {
    static VaccinationResponse from(Vaccination value) {
      return new VaccinationResponse(
          value.id(),
          value.vaccineName(),
          value.manufacturer(),
          value.batchNumber(),
          value.administeredOn(),
          value.nextDueOn());
    }
  }

  public record PrescriptionResponse(
      UUID id,
      UUID consultationId,
      String medication,
      String dosage,
      String frequency,
      String duration,
      String instructions,
      Instant issuedAt,
      LocalDate validUntil) {
    static PrescriptionResponse from(Prescription value) {
      return new PrescriptionResponse(
          value.id(),
          value.consultationId(),
          value.medication(),
          value.dosage(),
          value.frequency(),
          value.duration(),
          value.instructions(),
          value.issuedAt(),
          value.validUntil());
    }
  }

  public record AttachmentResponse(
      UUID id,
      UUID consultationId,
      String filename,
      String contentType,
      long sizeBytes,
      String sha256,
      Instant createdAt) {
    static AttachmentResponse from(ClinicalAttachment value) {
      return new AttachmentResponse(
          value.id(),
          value.consultationId(),
          value.originalFilename(),
          value.contentType(),
          value.sizeBytes(),
          value.sha256(),
          value.createdAt());
    }
  }

  public record HistoryResponse(
      UUID id,
      String eventType,
      UUID resourceId,
      String summary,
      Instant occurredAt,
      String recordedByName,
      Map<String, Object> details) {
    static HistoryResponse from(ClinicalHistoryEvent value) {
      return new HistoryResponse(
          value.id(),
          value.eventType(),
          value.resourceId(),
          value.summary(),
          value.occurredAt(),
          value.recordedByName(),
          value.details());
    }
  }
}
