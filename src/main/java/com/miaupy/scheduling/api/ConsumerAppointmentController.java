package com.miaupy.scheduling.api;

import com.miaupy.scheduling.application.AppointmentUseCase;
import com.miaupy.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consumer/me/appointments")
public class ConsumerAppointmentController {
  private final AppointmentUseCase useCase;

  public ConsumerAppointmentController(AppointmentUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping
  @Operation(
      summary = "Listar meus agendamentos",
      description = "Lista agendamentos vinculados ao ConsumerProfile autenticado.")
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
    return PageResponse.from(useCase.listConsumer(page, size), AppointmentResponse::from);
  }

  @PostMapping
  @Operation(
      summary = "Solicitar agendamento",
      description =
          "Solicita exatamente um slot público disponível para pet e cliente previamente vinculados à loja.")
  public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody Request request) {
    AppointmentResponse body = AppointmentResponse.from(useCase.createConsumer(request.command()));
    return ResponseEntity.created(URI.create("/api/v1/consumer/me/appointments/" + body.id()))
        .body(body);
  }

  @PostMapping("/{id}/cancel")
  @Operation(
      summary = "Cancelar meu agendamento",
      description = "Cancela somente um agendamento pertencente ao consumidor autenticado.")
  public AppointmentResponse cancel(
      @Parameter(description = "UUID do agendamento pertencente ao consumidor autenticado.")
          @PathVariable
          UUID id) {
    return AppointmentResponse.from(useCase.cancelConsumer(id));
  }

  public record Request(
      @NotBlank
          @Size(max = 80)
          @Schema(description = "Slug da loja pública selecionada pelo consumidor.")
          String storeSlug,
      @NotNull @Schema(description = "UUID do pet global pertencente ao consumidor autenticado.")
          UUID consumerPetId,
      @NotNull @Schema(description = "UUID do serviço selecionado.") UUID serviceId,
      @Schema(description = "UUID opcional do funcionário reservado para o atendimento.")
          UUID employeeId,
      @NotNull
          @Future
          @Schema(
              description = "Instante futuro em ISO-8601; deve corresponder a um slot disponível.",
              example = "2026-08-11T14:00:00Z")
          Instant startAt,
      @Size(max = 2000) @Schema(description = "Observações ou instruções do agendamento.")
          String notes) {
    AppointmentUseCase.ConsumerCommand command() {
      return new AppointmentUseCase.ConsumerCommand(
          storeSlug, consumerPetId, serviceId, employeeId, startAt, notes);
    }
  }
}
