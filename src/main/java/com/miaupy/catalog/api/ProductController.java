package com.miaupy.catalog.api;

import com.miaupy.catalog.application.ProductUseCase;
import com.miaupy.catalog.domain.Product;
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
@RequestMapping("/api/v1/business/products")
public class ProductController {
  private static final String WRITE =
      "hasAuthority('PRODUCT_WRITE') or hasAnyRole('OWNER','ADMIN','CATALOG_MANAGER')";
  private final ProductUseCase useCase;

  public ProductController(ProductUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping
  @Operation(
      summary = "Listar produtos",
      description = "Lista paginada de produtos ativos pertencentes ao tenant autenticado.")
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
      summary = "Criar produto",
      description = "Cria produto inicialmente não publicado no tenant autenticado.")
  @PreAuthorize(WRITE)
  public ResponseEntity<Response> create(@Valid @RequestBody Request request) {
    Response body = Response.from(useCase.create(request.command()));
    return ResponseEntity.created(URI.create("/api/v1/business/products/" + body.id())).body(body);
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Consultar produto",
      description = "Consulta produto utilizando obrigatoriamente id e tenant_id.")
  public Response get(
      @Parameter(description = "UUID do produto no tenant autenticado.") @PathVariable UUID id) {
    return Response.from(useCase.get(id));
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Atualizar produto",
      description = "Atualiza o produto e invalida o cache público aplicável.")
  @PreAuthorize(WRITE)
  public Response update(
      @Parameter(description = "UUID do produto no tenant autenticado.") @PathVariable UUID id,
      @Valid @RequestBody Request request) {
    return Response.from(useCase.update(id, request.command()));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Desativar produto",
      description = "Executa exclusão lógica e remove o item da vitrine.")
  @PreAuthorize(WRITE)
  public ResponseEntity<Void> delete(
      @Parameter(description = "UUID do produto no tenant autenticado.") @PathVariable UUID id) {
    useCase.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/publish")
  @Operation(
      summary = "Publicar produto",
      description = "Publica o item ativo na vitrine e persiste evento na outbox.")
  @PreAuthorize(WRITE)
  public Response publish(
      @Parameter(description = "UUID do produto no tenant autenticado.") @PathVariable UUID id) {
    return Response.from(useCase.publish(id));
  }

  @PostMapping("/{id}/unpublish")
  @Operation(
      summary = "Despublicar produto",
      description = "Remove o item da vitrine e invalida o cache público.")
  @PreAuthorize(WRITE)
  public Response unpublish(
      @Parameter(description = "UUID do produto no tenant autenticado.") @PathVariable UUID id) {
    return Response.from(useCase.unpublish(id));
  }

  public record Request(
      @Size(max = 80) @Schema(description = "Código de estoque único entre itens ativos do tenant.")
          String sku,
      @NotBlank @Size(max = 180) @Schema(description = "Nome comercial do produto.") String name,
      @Size(max = 3000) @Schema(description = "Descrição pública do produto.") String description,
      @NotNull
          @Positive
          @Digits(integer = 17, fraction = 2)
          @Schema(
              description = "Preço monetário positivo com até duas casas decimais.",
              example = "129.90")
          BigDecimal price,
      @Positive
          @Digits(integer = 17, fraction = 2)
          @Schema(
              description = "Preço promocional positivo e não superior ao preço regular.",
              example = "109.90")
          BigDecimal promotionalPrice,
      @PositiveOrZero
          @Schema(description = "Quantidade disponível em estoque, igual ou maior que zero.")
          int stockQuantity) {
    ProductUseCase.Command command() {
      return new ProductUseCase.Command(
          sku, name, description, price, promotionalPrice, stockQuantity);
    }
  }

  public record Response(
      UUID id,
      String sku,
      String name,
      String description,
      BigDecimal price,
      BigDecimal promotionalPrice,
      int stockQuantity,
      boolean active,
      boolean published) {
    static Response from(Product product) {
      return new Response(
          product.id(),
          product.sku(),
          product.name(),
          product.description(),
          product.price(),
          product.promotionalPrice(),
          product.stockQuantity(),
          product.active(),
          product.published());
    }
  }
}
