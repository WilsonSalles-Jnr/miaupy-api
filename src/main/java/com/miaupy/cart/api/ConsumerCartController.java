package com.miaupy.cart.api;

import com.miaupy.cart.application.CartUseCase;
import com.miaupy.cart.domain.Cart;
import com.miaupy.cart.domain.CartItem;
import com.miaupy.cart.domain.CartStatus;
import com.miaupy.order.api.OrderResponse;
import com.miaupy.order.application.CheckoutUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consumer/me/cart")
public class ConsumerCartController {
  private final CartUseCase cartUseCase;
  private final CheckoutUseCase checkoutUseCase;

  public ConsumerCartController(CartUseCase cartUseCase, CheckoutUseCase checkoutUseCase) {
    this.cartUseCase = cartUseCase;
    this.checkoutUseCase = checkoutUseCase;
  }

  @GetMapping
  @Operation(
      summary = "Consultar meu carrinho",
      description =
          "Retorna o carrinho ativo persistido do consumidor. Responde 204 quando ainda não existe carrinho.")
  public ResponseEntity<CartResponse> get() {
    return cartUseCase
        .get()
        .map(cart -> ResponseEntity.ok(CartResponse.from(cart)))
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @PostMapping("/items")
  @Operation(
      summary = "Adicionar produto ao carrinho",
      description =
          "Adiciona um produto publicado ao carrinho. O carrinho ativo aceita produtos de apenas uma empresa.")
  public CartResponse add(@Valid @RequestBody AddItemRequest request) {
    return CartResponse.from(
        cartUseCase.addItem(request.storeSlug(), request.productId(), request.quantity()));
  }

  @PatchMapping("/items/{itemId}")
  @Operation(
      summary = "Alterar quantidade do item",
      description = "Atualiza a quantidade usando o estoque e o preço atual do produto publicado.")
  public CartResponse update(
      @Parameter(description = "UUID do item pertencente ao carrinho do consumidor autenticado.")
          @PathVariable
          UUID itemId,
      @Valid @RequestBody UpdateItemRequest request) {
    return CartResponse.from(cartUseCase.updateItem(itemId, request.quantity()));
  }

  @DeleteMapping("/items/{itemId}")
  @Operation(
      summary = "Remover item do carrinho",
      description =
          "Remove somente um item pertencente ao carrinho ativo do consumidor autenticado.")
  public CartResponse remove(
      @Parameter(description = "UUID do item pertencente ao carrinho do consumidor autenticado.")
          @PathVariable
          UUID itemId) {
    return CartResponse.from(cartUseCase.removeItem(itemId));
  }

  @PostMapping("/checkout")
  @Operation(
      summary = "Finalizar carrinho",
      description =
          "Executa checkout idempotente, reserva estoque com lock no PostgreSQL, cria pedido com snapshots e persiste eventos na outbox na mesma transação.")
  public OrderResponse checkout(
      @Parameter(
              description =
                  "UUID único do checkout. Reutilize a mesma chave ao repetir a mesma solicitação.")
          @RequestHeader("Idempotency-Key")
          UUID idempotencyKey) {
    return OrderResponse.from(checkoutUseCase.checkout(idempotencyKey));
  }

  public record AddItemRequest(
      @NotBlank
          @Size(max = 80)
          @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*")
          @Schema(
              description = "Slug público da empresa fornecedora do produto.",
              example = "clinica-pet-centro")
          String storeSlug,
      @NotNull @Schema(description = "UUID do produto publicado selecionado.") UUID productId,
      @Positive @Schema(description = "Quantidade positiva a adicionar ao carrinho.", example = "2")
          int quantity) {}

  public record UpdateItemRequest(
      @Positive @Schema(description = "Nova quantidade positiva do item.", example = "3")
          int quantity) {}

  public record CartItemResponse(
      UUID id, UUID productId, int quantity, BigDecimal unitPrice, BigDecimal total) {
    static CartItemResponse from(CartItem item) {
      return new CartItemResponse(
          item.id(), item.productId(), item.quantity(), item.unitPrice(), item.total());
    }
  }

  public record CartResponse(
      UUID id,
      Long tenantId,
      CartStatus status,
      List<CartItemResponse> items,
      BigDecimal subtotal) {
    static CartResponse from(Cart cart) {
      return new CartResponse(
          cart.id(),
          cart.tenantId(),
          cart.status(),
          cart.items().stream().map(CartItemResponse::from).toList(),
          cart.subtotal());
    }
  }
}
