package com.miaupy.order.api;

import com.miaupy.order.application.OrderUseCase;
import com.miaupy.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consumer/me/orders")
public class ConsumerOrderController {
  private final OrderUseCase useCase;

  public ConsumerOrderController(OrderUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping
  @Operation(
      summary = "Listar meus pedidos",
      description = "Lista somente pedidos pertencentes ao ConsumerProfile autenticado.")
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
    return PageResponse.from(useCase.listConsumer(page, size), OrderResponse::from);
  }

  @GetMapping("/{orderId}")
  @Operation(
      summary = "Consultar meu pedido",
      description =
          "Retorna o pedido com snapshots dos produtos somente quando pertence ao consumidor autenticado.")
  public OrderResponse get(
      @Parameter(description = "UUID do pedido pertencente ao consumidor autenticado.")
          @PathVariable
          UUID orderId) {
    return OrderResponse.from(useCase.getConsumer(orderId));
  }
}
