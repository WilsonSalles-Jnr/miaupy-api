package com.miaupy.scheduling.api;

import com.miaupy.scheduling.application.AppointmentUseCase;
import com.miaupy.scheduling.domain.AppointmentStatus;
import com.miaupy.shared.api.PageResponse;
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
  public PageResponse<AppointmentResponse> list(
      @RequestParam(defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return PageResponse.from(useCase.listBusiness(page, size), AppointmentResponse::from);
  }

  @PostMapping
  @PreAuthorize("hasAuthority('APPOINTMENT_CREATE') or hasAnyRole('OWNER','ADMIN','RECEPTIONIST')")
  public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody Request request) {
    AppointmentResponse body = AppointmentResponse.from(useCase.createBusiness(request.command()));
    return ResponseEntity.created(URI.create("/api/v1/business/appointments/" + body.id()))
        .body(body);
  }

  @PostMapping("/{id}/confirm")
  @PreAuthorize("hasAuthority('APPOINTMENT_CONFIRM') or hasAnyRole('OWNER','ADMIN','RECEPTIONIST')")
  public AppointmentResponse confirm(@PathVariable UUID id) {
    return transition(id, AppointmentStatus.CONFIRMED);
  }

  @PostMapping("/{id}/reject")
  @PreAuthorize("hasAuthority('APPOINTMENT_CONFIRM') or hasAnyRole('OWNER','ADMIN','RECEPTIONIST')")
  public AppointmentResponse reject(@PathVariable UUID id) {
    return transition(id, AppointmentStatus.REJECTED);
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('APPOINTMENT_CANCEL') or hasAnyRole('OWNER','ADMIN','RECEPTIONIST')")
  public AppointmentResponse cancel(@PathVariable UUID id) {
    return transition(id, AppointmentStatus.CANCELLED);
  }

  @PostMapping("/{id}/start")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN','RECEPTIONIST','VETERINARIAN','GROOMER')")
  public AppointmentResponse start(@PathVariable UUID id) {
    return transition(id, AppointmentStatus.IN_PROGRESS);
  }

  @PostMapping("/{id}/complete")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN','RECEPTIONIST','VETERINARIAN','GROOMER')")
  public AppointmentResponse complete(@PathVariable UUID id) {
    return transition(id, AppointmentStatus.COMPLETED);
  }

  @PostMapping("/{id}/no-show")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN','RECEPTIONIST')")
  public AppointmentResponse noShow(@PathVariable UUID id) {
    return transition(id, AppointmentStatus.NO_SHOW);
  }

  private AppointmentResponse transition(UUID id, AppointmentStatus target) {
    return AppointmentResponse.from(useCase.transitionBusiness(id, target));
  }

  public record Request(
      @NotNull UUID customerId,
      @NotNull UUID petId,
      @NotNull UUID serviceId,
      UUID employeeId,
      @NotNull @Future Instant startAt,
      @Size(max = 2000) String notes) {
    AppointmentUseCase.Command command() {
      return new AppointmentUseCase.Command(
          customerId, petId, serviceId, employeeId, startAt, notes);
    }
  }
}
