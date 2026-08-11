package com.miaupy.catalog.api;

import com.miaupy.catalog.application.OfferedServiceUseCase;
import com.miaupy.catalog.domain.OfferedService;
import com.miaupy.shared.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business/services")
public class OfferedServiceController {
  private static final String WRITE =
      "hasAuthority('SERVICE_WRITE') or hasAnyRole('OWNER','ADMIN','CATALOG_MANAGER')";
  private final OfferedServiceUseCase useCase;

  public OfferedServiceController(OfferedServiceUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping
  public PageResponse<Response> list(
      @RequestParam(defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return PageResponse.from(useCase.list(page, size), Response::from);
  }

  @PostMapping
  @PreAuthorize(WRITE)
  public ResponseEntity<Response> create(@Valid @RequestBody Request request) {
    Response body = Response.from(useCase.create(request.command()));
    return ResponseEntity.created(URI.create("/api/v1/business/services/" + body.id())).body(body);
  }

  @GetMapping("/{id}")
  public Response get(@PathVariable UUID id) {
    return Response.from(useCase.get(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize(WRITE)
  public Response update(@PathVariable UUID id, @Valid @RequestBody Request request) {
    return Response.from(useCase.update(id, request.command()));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize(WRITE)
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    useCase.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/publish")
  @PreAuthorize(WRITE)
  public Response publish(@PathVariable UUID id) {
    return Response.from(useCase.publish(id));
  }

  @PostMapping("/{id}/unpublish")
  @PreAuthorize(WRITE)
  public Response unpublish(@PathVariable UUID id) {
    return Response.from(useCase.unpublish(id));
  }

  public record Request(
      @NotBlank @Size(max = 180) String name,
      @Size(max = 3000) String description,
      @Min(1) @Max(1440) int durationMinutes,
      @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal price,
      boolean requiresApproval) {
    OfferedServiceUseCase.Command command() {
      return new OfferedServiceUseCase.Command(
          name, description, durationMinutes, price, requiresApproval);
    }
  }

  public record Response(
      UUID id,
      String name,
      String description,
      int durationMinutes,
      BigDecimal price,
      boolean active,
      boolean published,
      boolean requiresApproval) {
    static Response from(OfferedService service) {
      return new Response(
          service.id(),
          service.name(),
          service.description(),
          service.durationMinutes(),
          service.price(),
          service.active(),
          service.published(),
          service.requiresApproval());
    }
  }
}
