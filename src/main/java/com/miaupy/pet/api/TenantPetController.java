package com.miaupy.pet.api;

import com.miaupy.pet.application.TenantPetUseCase;
import com.miaupy.pet.domain.*;
import com.miaupy.shared.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/business")
public class TenantPetController {
  private final TenantPetUseCase useCase;

  public TenantPetController(TenantPetUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping("/customers/{customerId}/pets")
  public PageResponse<Response> list(
      @PathVariable UUID customerId,
      @RequestParam(defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return PageResponse.from(useCase.list(customerId, page, size), Response::from);
  }

  @PostMapping("/customers/{customerId}/pets")
  @PreAuthorize(
      "hasAuthority('PET_WRITE') or hasAnyRole('OWNER','ADMIN','RECEPTIONIST','VETERINARIAN')")
  public ResponseEntity<Response> create(
      @PathVariable UUID customerId, @Valid @RequestBody Request r) {
    Response body = Response.from(useCase.create(customerId, r.command()));
    return ResponseEntity.created(URI.create("/api/v1/business/pets/" + body.id())).body(body);
  }

  @GetMapping("/pets/{id}")
  public Response get(@PathVariable UUID id) {
    return Response.from(useCase.get(id));
  }

  @PutMapping("/pets/{id}")
  @PreAuthorize(
      "hasAuthority('PET_WRITE') or hasAnyRole('OWNER','ADMIN','RECEPTIONIST','VETERINARIAN')")
  public Response update(@PathVariable UUID id, @Valid @RequestBody Request r) {
    return Response.from(useCase.update(id, r.command()));
  }

  @DeleteMapping("/pets/{id}")
  @PreAuthorize("hasAuthority('PET_WRITE') or hasAnyRole('OWNER','ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    useCase.delete(id);
    return ResponseEntity.noContent().build();
  }

  public record Request(
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Size(max = 60) String species,
      @Size(max = 120) String breed,
      @PastOrPresent LocalDate birthDate,
      @NotNull PetSex sex,
      @Positive @Digits(integer = 6, fraction = 2) BigDecimal weight,
      @Size(max = 2000) String notes) {
    TenantPetUseCase.Command command() {
      return new TenantPetUseCase.Command(name, species, breed, birthDate, sex, weight, notes);
    }
  }

  public record Response(
      UUID id,
      UUID customerId,
      UUID consumerPetId,
      String name,
      String species,
      String breed,
      LocalDate birthDate,
      PetSex sex,
      BigDecimal weight,
      String notes,
      boolean active) {
    static Response from(TenantPet p) {
      return new Response(
          p.id(),
          p.tenantCustomerId(),
          p.consumerPetId(),
          p.name(),
          p.species(),
          p.breed(),
          p.birthDate(),
          p.sex(),
          p.weight(),
          p.notes(),
          p.active());
    }
  }
}
