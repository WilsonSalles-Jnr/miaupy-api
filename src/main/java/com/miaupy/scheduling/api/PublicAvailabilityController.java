package com.miaupy.scheduling.api;

import com.miaupy.scheduling.application.AvailabilityUseCase;
import com.miaupy.scheduling.application.AvailabilityUseCase.AvailableSlot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/stores/{slug}/availability")
public class PublicAvailabilityController {
  private final AvailabilityUseCase useCase;

  public PublicAvailabilityController(AvailabilityUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping
  @Operation(
      summary = "Consultar horários disponíveis",
      description =
          "Calcula slots livres no timezone da empresa, removendo intervalos já ocupados.")
  public List<AvailableSlot> availability(
      @Parameter(description = "Slug público único da loja.") @PathVariable String slug,
      @Parameter(description = "UUID do serviço publicado pela loja.") @RequestParam @NotNull
          UUID serviceId,
      @Parameter(description = "Data local da empresa no formato YYYY-MM-DD.")
          @RequestParam
          @NotNull
          @FutureOrPresent
          LocalDate date,
      @Parameter(description = "UUID opcional do funcionário usado para filtrar a disponibilidade.")
          @RequestParam(required = false)
          UUID employeeId) {
    return useCase.publicAvailability(slug, serviceId, date, employeeId);
  }
}
