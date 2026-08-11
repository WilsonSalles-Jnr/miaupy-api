package com.miaupy.customer.api;

import com.miaupy.customer.application.TenantCustomerUseCase;
import com.miaupy.customer.domain.TenantCustomer;
import com.miaupy.shared.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/business/customers")
public class TenantCustomerController {
  private final TenantCustomerUseCase useCase;

  public TenantCustomerController(TenantCustomerUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping
  public PageResponse<Response> list(
      @RequestParam(defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return PageResponse.from(useCase.list(page, size), Response::from);
  }

  @PostMapping
  @PreAuthorize("hasAuthority('CUSTOMER_WRITE') or hasAnyRole('OWNER','ADMIN','RECEPTIONIST')")
  public ResponseEntity<Response> create(@Valid @RequestBody Request r) {
    Response body = Response.from(useCase.create(r.command()));
    return ResponseEntity.created(URI.create("/api/v1/business/customers/" + body.id())).body(body);
  }

  @GetMapping("/{id}")
  public Response get(@PathVariable UUID id) {
    return Response.from(useCase.get(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('CUSTOMER_WRITE') or hasAnyRole('OWNER','ADMIN','RECEPTIONIST')")
  public Response update(@PathVariable UUID id, @Valid @RequestBody Request r) {
    return Response.from(useCase.update(id, r.command()));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('CUSTOMER_WRITE') or hasAnyRole('OWNER','ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    useCase.delete(id);
    return ResponseEntity.noContent().build();
  }

  public record Request(
      @NotBlank @Size(max = 160) String name,
      @Email @Size(max = 254) String email,
      @Size(max = 32) String phone,
      @Size(max = 32) String document,
      @Size(max = 2000) String notes) {
    TenantCustomerUseCase.Command command() {
      return new TenantCustomerUseCase.Command(name, email, phone, document, notes);
    }
  }

  public record Response(
      UUID id,
      UUID consumerProfileId,
      String name,
      String email,
      String phone,
      String document,
      String notes,
      boolean active) {
    static Response from(TenantCustomer c) {
      return new Response(
          c.id(),
          c.consumerProfileId(),
          c.name(),
          c.email(),
          c.phone(),
          c.document(),
          c.notes(),
          c.active());
    }
  }
}
