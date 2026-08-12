package com.miaupy.scheduling.api;

import com.miaupy.scheduling.application.AvailabilityUseCase;
import com.miaupy.scheduling.domain.AvailabilityRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business/availability-rules")
public class AvailabilityController {
  private final AvailabilityUseCase useCase;

  public AvailabilityController(AvailabilityUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping
  @Operation(
      summary = "Listar regras de disponibilidade",
      description = "Lista regras semanais ativas do tenant autenticado.")
  public List<Response> list() {
    return useCase.listRules().stream().map(Response::from).toList();
  }

  @PostMapping
  @Operation(
      summary = "Criar regra de disponibilidade",
      description = "Cria intervalo semanal de atendimento geral ou de um funcionário.")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN','SCHEDULING_MANAGER')")
  public ResponseEntity<Response> create(@Valid @RequestBody Request request) {
    Response body =
        Response.from(
            useCase.createRule(
                request.employeeId(),
                request.dayOfWeek(),
                request.startLocal(),
                request.endLocal()));
    return ResponseEntity.created(URI.create("/api/v1/business/availability-rules/" + body.id()))
        .body(body);
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Desativar regra de disponibilidade",
      description = "Desativa uma regra semanal pertencente ao tenant.")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN','SCHEDULING_MANAGER')")
  public ResponseEntity<Void> delete(
      @Parameter(description = "UUID da regra de disponibilidade no tenant autenticado.")
          @PathVariable
          UUID id) {
    useCase.deleteRule(id);
    return ResponseEntity.noContent().build();
  }

  public record Request(
      @Schema(
              description =
                  "UUID opcional do funcionário; ausente representa disponibilidade geral.")
          UUID employeeId,
      @NotNull @Schema(description = "Dia da semana ISO, de MONDAY a SUNDAY.") DayOfWeek dayOfWeek,
      @NotNull @Schema(description = "Horário local inicial no formato HH:mm:ss.")
          LocalTime startLocal,
      @NotNull @Schema(description = "Horário local final no formato HH:mm:ss.")
          LocalTime endLocal) {}

  public record Response(
      UUID id, UUID employeeId, DayOfWeek dayOfWeek, LocalTime startLocal, LocalTime endLocal) {
    static Response from(AvailabilityRule rule) {
      return new Response(
          rule.id(), rule.employeeId(), rule.dayOfWeek(), rule.startLocal(), rule.endLocal());
    }
  }
}
