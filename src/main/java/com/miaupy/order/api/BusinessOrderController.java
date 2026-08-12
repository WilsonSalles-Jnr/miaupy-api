package com.miaupy.order.api;

import com.miaupy.order.application.OrderUseCase;
import com.miaupy.order.domain.OrderStatus;
import com.miaupy.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business/orders")
public class BusinessOrderController {
  private static final String WRITE =
      "hasAuthority('ORDER_WRITE') or hasAnyRole('OWNER','ADMIN','ORDER_MANAGER')";
  private final OrderUseCase useCase;

  public BusinessOrderController(OrderUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('ORDER_READ') or hasAnyRole('OWNER','ADMIN','ORDER_MANAGER')")
  @Operation(
      summary = "Listar pedidos da empresa",
      description = "Lista pedidos filtrados obrigatoriamente pelo tenant obtido do JWT.")
  public PageResponse<OrderResponse> list(
      @Parameter(description = "Índice da página, iniciando em zero.")
          @RequestParam(defaultValue = "0")
          @PositiveOrZero
          int page,
      @Parameter(description = "Quantidade de elementos por página, entre 1 e 100.")
          @RequestParam(defaultValue = "20")
          @Min(1)
          @Max(100)
          int size) {
    return PageResponse.from(useCase.listBusiness(page, size), OrderResponse::from);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('ORDER_READ') or hasAnyRole('OWNER','ADMIN','ORDER_MANAGER')")
  @Operation(
      summary = "Consultar pedido da empresa",
      description = "Consulta o pedido utilizando obrigatoriamente id e tenant_id.")
  public OrderResponse get(
      @Parameter(description = "UUID do pedido dentro do tenant autenticado.") @PathVariable
          UUID id) {
    return OrderResponse.from(useCase.getBusiness(id));
  }

  @PostMapping("/{id}/processing")
  @PreAuthorize(WRITE)
  @Operation(
      summary = "Iniciar processamento do pedido",
      description = "Transiciona um pedido CREATED para PROCESSING.")
  public OrderResponse processing(
      @Parameter(description = "UUID do pedido dentro do tenant autenticado.") @PathVariable
          UUID id) {
    return transition(id, OrderStatus.PROCESSING);
  }

  @PostMapping("/{id}/ready")
  @PreAuthorize(WRITE)
  @Operation(
      summary = "Marcar pedido como pronto",
      description = "Transiciona um pedido PROCESSING para READY.")
  public OrderResponse ready(
      @Parameter(description = "UUID do pedido dentro do tenant autenticado.") @PathVariable
          UUID id) {
    return transition(id, OrderStatus.READY);
  }

  @PostMapping("/{id}/complete")
  @PreAuthorize(WRITE)
  @Operation(
      summary = "Concluir pedido",
      description = "Transiciona um pedido READY para COMPLETED.")
  public OrderResponse complete(
      @Parameter(description = "UUID do pedido dentro do tenant autenticado.") @PathVariable
          UUID id) {
    return transition(id, OrderStatus.COMPLETED);
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize(WRITE)
  @Operation(
      summary = "Cancelar pedido",
      description =
          "Cancela pedido ainda não concluído e devolve ao estoque as quantidades reservadas.")
  public OrderResponse cancel(
      @Parameter(description = "UUID do pedido dentro do tenant autenticado.") @PathVariable
          UUID id) {
    return transition(id, OrderStatus.CANCELLED);
  }

  private OrderResponse transition(UUID id, OrderStatus status) {
    return OrderResponse.from(useCase.transitionBusiness(id, status));
  }
}
