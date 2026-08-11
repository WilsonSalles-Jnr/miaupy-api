package com.miaupy.scheduling.api;

import com.miaupy.scheduling.application.AppointmentUseCase;
import com.miaupy.shared.api.PageResponse;
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
  public PageResponse<AppointmentResponse> list(
      @RequestParam(defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return PageResponse.from(useCase.listConsumer(page, size), AppointmentResponse::from);
  }

  @PostMapping
  public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody Request request) {
    AppointmentResponse body = AppointmentResponse.from(useCase.createConsumer(request.command()));
    return ResponseEntity.created(URI.create("/api/v1/consumer/me/appointments/" + body.id()))
        .body(body);
  }

  @PostMapping("/{id}/cancel")
  public AppointmentResponse cancel(@PathVariable UUID id) {
    return AppointmentResponse.from(useCase.cancelConsumer(id));
  }

  public record Request(
      @NotBlank @Size(max = 80) String storeSlug,
      @NotNull UUID consumerPetId,
      @NotNull UUID serviceId,
      UUID employeeId,
      @NotNull @Future Instant startAt,
      @Size(max = 2000) String notes) {
    AppointmentUseCase.ConsumerCommand command() {
      return new AppointmentUseCase.ConsumerCommand(
          storeSlug, consumerPetId, serviceId, employeeId, startAt, notes);
    }
  }
}
