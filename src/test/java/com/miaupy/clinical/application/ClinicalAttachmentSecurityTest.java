package com.miaupy.clinical.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miaupy.clinical.domain.ClinicalAttachment;
import com.miaupy.clinical.domain.ClinicalHistoryEvent;
import com.miaupy.clinical.domain.ClinicalRepository;
import com.miaupy.integration.outbox.OutboxWriter;
import com.miaupy.pet.domain.TenantPet;
import com.miaupy.pet.domain.TenantPetRepository;
import com.miaupy.scheduling.domain.AppointmentRepository;
import com.miaupy.shared.security.ActorContext;
import com.miaupy.shared.security.ActorType;
import com.miaupy.shared.security.AuthenticatedActor;
import com.miaupy.shared.tenant.TenantContext;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ClinicalAttachmentSecurityTest {
  @Test
  void acceptsRealPdfSanitizesFilenameAndPersistsHistoryAndOutbox() {
    Fixtures f = new Fixtures();
    byte[] pdf = "%PDF-1.7\ncontent".getBytes(StandardCharsets.US_ASCII);

    ClinicalAttachment saved =
        f.useCase.uploadAttachment(f.petId, null, "../../exam\r\n.pdf", "application/pdf", pdf);

    assertThat(saved.originalFilename()).isEqualTo("exam.pdf");
    assertThat(saved.sha256()).hasSize(64);
    assertThat(saved.content()).containsExactly(pdf);
    ArgumentCaptor<ClinicalHistoryEvent> history =
        ArgumentCaptor.forClass(ClinicalHistoryEvent.class);
    verify(f.clinical).appendHistory(history.capture());
    assertThat(history.getValue().recordedByName()).isEqualTo("Dra. Maria");
    assertThat(history.getValue().details())
        .containsEntry("filename", "exam.pdf")
        .containsEntry("contentType", "application/pdf");
    verify(f.outbox).append(any(), any(), any(), any(), any());
  }

  @Test
  void rejectsContentWhoseSignatureDoesNotMatchDeclaredType() {
    Fixtures f = new Fixtures();

    assertThatThrownBy(
            () ->
                f.useCase.uploadAttachment(
                    f.petId,
                    null,
                    "fake.pdf",
                    "application/pdf",
                    "not-a-pdf".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not match");
  }

  private static final class Fixtures {
    private final Long tenantId = 101L;
    private final UUID petId = UUID.randomUUID();
    private final TenantContext tenants = mock(TenantContext.class);
    private final ActorContext actors = mock(ActorContext.class);
    private final TenantPetRepository pets = mock(TenantPetRepository.class);
    private final ClinicalRepository clinical = mock(ClinicalRepository.class);
    private final OutboxWriter outbox = mock(OutboxWriter.class);
    private final ClinicalUseCase useCase;

    private Fixtures() {
      when(tenants.getRequiredTenantId()).thenReturn(tenantId);
      when(pets.findByIdAndTenantId(petId, tenantId))
          .thenReturn(Optional.of(mock(TenantPet.class)));
      when(actors.getRequiredActor())
          .thenReturn(new AuthenticatedActor("veterinarian-subject", ActorType.B2B));
      when(actors.getRequiredActorDisplayName()).thenReturn("Dra. Maria");
      when(clinical.save(any(ClinicalAttachment.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      useCase =
          new ClinicalUseCase(
              tenants, actors, pets, mock(AppointmentRepository.class), clinical, outbox);
    }
  }
}
