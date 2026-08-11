package com.miaupy.scheduling.api;

import com.miaupy.scheduling.application.AvailabilityUseCase;
import com.miaupy.scheduling.application.AvailabilityUseCase.AvailableSlot;
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
  public List<AvailableSlot> availability(
      @PathVariable String slug,
      @RequestParam @NotNull UUID serviceId,
      @RequestParam @NotNull @FutureOrPresent LocalDate date,
      @RequestParam(required = false) UUID employeeId) {
    return useCase.publicAvailability(slug, serviceId, date, employeeId);
  }
}
