package com.miaupy.customer.api;

import com.miaupy.customer.application.TenantCustomerUseCase;
import com.miaupy.customer.domain.TenantCustomer;
import com.miaupy.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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
  @Operation(
      summary = "Listar clientes",
      description = "Lista paginada do CRM filtrada pelo tenant do JWT.")
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
      summary = "Cadastrar cliente",
      description = "Cadastra cliente no CRM do tenant sem realizar vínculo B2C automático.")
  @PreAuthorize("hasAuthority('CUSTOMER_WRITE') or hasAnyRole('OWNER','ADMIN','RECEPTIONIST')")
  public ResponseEntity<Response> create(@Valid @RequestBody Request r) {
    Response body = Response.from(useCase.create(r.command()));
    return ResponseEntity.created(URI.create("/api/v1/business/customers/" + body.id())).body(body);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Consultar cliente",
      description = "Consulta cliente utilizando obrigatoriamente id e tenant_id.")
  public Response get(
      @Parameter(description = "UUID do cliente no tenant autenticado.") @PathVariable UUID id) {
    return Response.from(useCase.get(id));
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Atualizar cliente",
      description = "Atualiza os dados do cliente dentro do tenant autenticado.")
  @PreAuthorize("hasAuthority('CUSTOMER_WRITE') or hasAnyRole('OWNER','ADMIN','RECEPTIONIST')")
  public Response update(
      @Parameter(description = "UUID do cliente no tenant autenticado.") @PathVariable UUID id,
      @Valid @RequestBody Request r) {
    return Response.from(useCase.update(id, r.command()));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Desativar cliente",
      description = "Realiza exclusão lógica do cliente do CRM.")
  @PreAuthorize("hasAuthority('CUSTOMER_WRITE') or hasAnyRole('OWNER','ADMIN')")
  public ResponseEntity<Void> delete(
      @Parameter(description = "UUID do cliente no tenant autenticado.") @PathVariable UUID id) {
    useCase.delete(id);
    return ResponseEntity.noContent().build();
  }

  public record Request(
      @NotBlank @Size(max = 160) @Schema(description = "Nome completo do cliente.") String name,
      @Email @Size(max = 254) @Schema(description = "Endereço de e-mail válido.") String email,
      @Size(max = 32) @Schema(description = "Telefone de contato.") String phone,
      @Size(max = 32) @Schema(description = "Documento pessoal do cliente.") String document,
      @Size(max = 2000) @Schema(description = "Observações internas sobre o cliente.")
          String notes) {
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
