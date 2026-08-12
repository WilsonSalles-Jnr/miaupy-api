package com.miaupy.catalog.api;

import com.miaupy.catalog.application.OfferedServiceUseCase;
import com.miaupy.catalog.domain.OfferedService;
import com.miaupy.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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
  @Operation(
      summary = "Listar serviços",
      description = "Lista paginada de serviços ativos pertencentes ao tenant autenticado.")
  public PageResponse<Response> list(
      @Parameter(description = "Índice da página, iniciando em zero.")
          @RequestParam(defaultValue = "0")
          @PositiveOrZero
          int page,
      @Parameter(description = "Quantidade de elementos por página, entre 1 e 100.")
          @RequestParam(defaultValue = "20")
          @Min(1)
          @Max(100)
          int size) {
    return PageResponse.from(useCase.list(page, size), Response::from);
  }

  @PostMapping
  @Operation(
      summary = "Criar serviço",
      description = "Cria serviço inicialmente não publicado no tenant autenticado.")
  @PreAuthorize(WRITE)
  public ResponseEntity<Response> create(@Valid @RequestBody Request request) {
    Response body = Response.from(useCase.create(request.command()));
    return ResponseEntity.created(URI.create("/api/v1/business/services/" + body.id())).body(body);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Consultar serviço",
      description = "Consulta serviço utilizando obrigatoriamente id e tenant_id.")
  public Response get(
      @Parameter(description = "UUID do serviço no tenant autenticado.") @PathVariable UUID id) {
    return Response.from(useCase.get(id));
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Atualizar serviço",
      description = "Atualiza o serviço e invalida o cache público aplicável.")
  @PreAuthorize(WRITE)
  public Response update(
      @Parameter(description = "UUID do serviço no tenant autenticado.") @PathVariable UUID id,
      @Valid @RequestBody Request request) {
    return Response.from(useCase.update(id, request.command()));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Desativar serviço",
      description = "Executa exclusão lógica e remove o item da vitrine.")
  @PreAuthorize(WRITE)
  public ResponseEntity<Void> delete(
      @Parameter(description = "UUID do serviço no tenant autenticado.") @PathVariable UUID id) {
    useCase.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/publish")
  @Operation(
      summary = "Publicar serviço",
      description = "Publica o item ativo na vitrine e persiste evento na outbox.")
  @PreAuthorize(WRITE)
  public Response publish(
      @Parameter(description = "UUID do serviço no tenant autenticado.") @PathVariable UUID id) {
    return Response.from(useCase.publish(id));
  }

  @PostMapping("/{id}/unpublish")
  @Operation(
      summary = "Despublicar serviço",
      description = "Remove o item da vitrine e invalida o cache público.")
  @PreAuthorize(WRITE)
  public Response unpublish(
      @Parameter(description = "UUID do serviço no tenant autenticado.") @PathVariable UUID id) {
    return Response.from(useCase.unpublish(id));
  }

  public record Request(
      @NotBlank @Size(max = 180) @Schema(description = "Nome comercial do serviço.") String name,
      @Size(max = 3000) @Schema(description = "Descrição pública do serviço.") String description,
      @Min(1) @Max(1440) @Schema(description = "Duração positiva do serviço em minutos.")
          int durationMinutes,
      @NotNull
          @Positive
          @Digits(integer = 17, fraction = 2)
          @Schema(
              description = "Preço monetário positivo com até duas casas decimais.",
              example = "129.90")
          BigDecimal price,
      @Schema(description = "Indica se o serviço requer aprovação do estabelecimento.")
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
