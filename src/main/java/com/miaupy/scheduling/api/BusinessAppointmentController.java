package com.miaupy.scheduling.api;

import com.miaupy.scheduling.application.AppointmentUseCase;
import com.miaupy.scheduling.domain.AppointmentStatus;
import com.miaupy.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business/appointments")
public class BusinessAppointmentController {
  private final AppointmentUseCase useCase;

  public BusinessAppointmentController(AppointmentUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping
  @Operation(
      summary = "Listar agendamentos do tenant",
      description = "Lista paginada de agendamentos pertencentes ao tenant autenticado.")
  public PageResponse<AppointmentResponse> list(
      @Parameter(description = "Índice da página, iniciando em zero.")
          @RequestParam(defaultValue = "0")
          @PositiveOrZero
          int page,
      @Parameter(description = "Quantidade de elementos por página, entre 1 e 100.")
          @RequestParam(defaultValue = "20")
          @Min(1)
          @Max(100)
          int size) {
    return PageResponse.from(useCase.listBusiness(page, size), AppointmentResponse::from);
  }

  @PostMapping
  @Operation(
      summary = "Criar agendamento empresarial",
      description =
          "Cria agendamento confirmado; duração é derivada do serviço e conflitos são protegidos no PostgreSQL.")
  @PreAuthorize("hasAuthority('APPOINTMENT_CREATE') or hasAnyRole('OWNER','ADMIN','RECEPTIONIST')")
  public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody Request request) {
    AppointmentResponse body = AppointmentResponse.from(useCase.createBusiness(request.command()));
    return ResponseEntity.created(URI.create("/api/v1/business/appointments/" + body.id()))
        .body(body);
  }

  @PostMapping("/{id}/confirm")
  @Operation(
      summary = "Confirmar agendamento",
      description = "Transiciona agendamento REQUESTED para CONFIRMED.")
  @PreAuthorize("hasAuthority('APPOINTMENT_CONFIRM') or hasAnyRole('OWNER','ADMIN','RECEPTIONIST')")
  public AppointmentResponse confirm(
      @Parameter(description = "UUID do agendamento no tenant autenticado.") @PathVariable
          UUID id) {
    return transition(id, AppointmentStatus.CONFIRMED);
  }

  @PostMapping("/{id}/reject")
  @Operation(
      summary = "Rejeitar agendamento",
      description = "Transiciona agendamento REQUESTED para REJECTED.")
  @PreAuthorize("hasAuthority('APPOINTMENT_CONFIRM') or hasAnyRole('OWNER','ADMIN','RECEPTIONIST')")
  public AppointmentResponse reject(
      @Parameter(description = "UUID do agendamento no tenant autenticado.") @PathVariable
          UUID id) {
    return transition(id, AppointmentStatus.REJECTED);
  }

  @PostMapping("/{id}/cancel")
  @Operation(
      summary = "Cancelar agendamento",
      description = "Cancela agendamento REQUESTED ou CONFIRMED.")
  @PreAuthorize("hasAuthority('APPOINTMENT_CANCEL') or hasAnyRole('OWNER','ADMIN','RECEPTIONIST')")
  public AppointmentResponse cancel(
      @Parameter(description = "UUID do agendamento no tenant autenticado.") @PathVariable
          UUID id) {
    return transition(id, AppointmentStatus.CANCELLED);
  }

  @PostMapping("/{id}/start")
  @Operation(
      summary = "Iniciar atendimento",
      description = "Transiciona agendamento CONFIRMED para IN_PROGRESS.")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN','RECEPTIONIST','VETERINARIAN','GROOMER')")
  public AppointmentResponse start(
      @Parameter(description = "UUID do agendamento no tenant autenticado.") @PathVariable
          UUID id) {
    return transition(id, AppointmentStatus.IN_PROGRESS);
  }

  @PostMapping("/{id}/complete")
  @Operation(
      summary = "Concluir atendimento",
      description = "Transiciona agendamento IN_PROGRESS para COMPLETED.")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN','RECEPTIONIST','VETERINARIAN','GROOMER')")
  public AppointmentResponse complete(
      @Parameter(description = "UUID do agendamento no tenant autenticado.") @PathVariable
          UUID id) {
    return transition(id, AppointmentStatus.COMPLETED);
  }

  @PostMapping("/{id}/no-show")
  @Operation(
      summary = "Registrar ausência",
      description = "Transiciona agendamento CONFIRMED para NO_SHOW.")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN','RECEPTIONIST')")
  public AppointmentResponse noShow(
      @Parameter(description = "UUID do agendamento no tenant autenticado.") @PathVariable
          UUID id) {
    return transition(id, AppointmentStatus.NO_SHOW);
  }

  private AppointmentResponse transition(UUID id, AppointmentStatus target) {
    return AppointmentResponse.from(useCase.transitionBusiness(id, target));
  }

  public record Request(
      @NotNull @Schema(description = "UUID do cliente do tenant.") UUID customerId,
      @NotNull @Schema(description = "UUID do pet interno do tenant.") UUID petId,
      @NotNull @Schema(description = "UUID do serviço selecionado.") UUID serviceId,
      @Schema(description = "UUID opcional do funcionário reservado para o atendimento.")
          UUID employeeId,
      @NotNull
          @Future
          @Schema(
              description = "Instante futuro em ISO-8601 UTC ou com offset.",
              example = "2026-08-11T14:00:00Z")
          Instant startAt,
      @Size(max = 2000) @Schema(description = "Observações ou instruções do agendamento.")
          String notes) {
    AppointmentUseCase.Command command() {
      return new AppointmentUseCase.Command(
          customerId, petId, serviceId, employeeId, startAt, notes);
    }
  }
}
