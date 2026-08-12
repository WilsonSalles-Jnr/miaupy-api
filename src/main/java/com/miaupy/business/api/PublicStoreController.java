package com.miaupy.business.api;

import com.miaupy.business.application.GetPublicBusinessUseCase;
import com.miaupy.catalog.application.PublicCatalogModels.ProductPage;
import com.miaupy.catalog.application.PublicCatalogModels.PublicProduct;
import com.miaupy.catalog.application.PublicCatalogModels.ServicePage;
import com.miaupy.catalog.application.PublicCatalogUseCase;
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
@RequestMapping("/api/v1/public/stores")
public class PublicStoreController {

  private final GetPublicBusinessUseCase getPublicBusiness;
  private final PublicCatalogUseCase publicCatalog;

  public PublicStoreController(
      GetPublicBusinessUseCase getPublicBusiness, PublicCatalogUseCase publicCatalog) {
    this.getPublicBusiness = getPublicBusiness;
    this.publicCatalog = publicCatalog;
  }

  @GetMapping("/{slug}")
  @Operation(
      summary = "Consultar loja pública",
      description = "Retorna somente empresa ativa e marcada como publicamente visível.")
  public PublicStoreResponse getBySlug(
      @Parameter(description = "Slug público único da loja.", example = "clinica-pet-centro")
          @PathVariable
          String slug) {
    return PublicStoreResponse.from(getPublicBusiness.execute(slug));
  }

  @GetMapping("/{slug}/products")
  @Operation(
      summary = "Listar produtos públicos",
      description =
          "Lista paginada de produtos ativos e publicados, com cache Redis e fallback PostgreSQL.")
  public ProductPage products(
      @Parameter(description = "Slug público único da loja.", example = "clinica-pet-centro")
          @PathVariable
          String slug,
      @Parameter(description = "Índice da página, iniciando em zero.", example = "0")
          @RequestParam(defaultValue = "0")
          @PositiveOrZero
          int page,
      @Parameter(description = "Quantidade de elementos por página, entre 1 e 100.", example = "20")
          @RequestParam(defaultValue = "20")
          @Min(1)
          @Max(100)
          int size) {
    return publicCatalog.products(slug, page, size);
  }

  @GetMapping("/{slug}/products/{productId}")
  @Operation(
      summary = "Consultar produto público",
      description = "Retorna um produto ativo e publicado pertencente à loja informada.")
  public PublicProduct product(
      @Parameter(description = "Slug público único da loja.", example = "clinica-pet-centro")
          @PathVariable
          String slug,
      @Parameter(description = "UUID do produto público pertencente à loja informada.")
          @PathVariable
          UUID productId) {
    return publicCatalog.product(slug, productId);
  }

  @GetMapping("/{slug}/services")
  @Operation(
      summary = "Listar serviços públicos",
      description = "Lista paginada de serviços ativos e publicados da loja.")
  public ServicePage services(
      @Parameter(description = "Slug público único da loja.", example = "clinica-pet-centro")
          @PathVariable
          String slug,
      @Parameter(description = "Índice da página, iniciando em zero.", example = "0")
          @RequestParam(defaultValue = "0")
          @PositiveOrZero
          int page,
      @Parameter(description = "Quantidade de elementos por página, entre 1 e 100.", example = "20")
          @RequestParam(defaultValue = "20")
          @Min(1)
          @Max(100)
          int size) {
    return publicCatalog.services(slug, page, size);
  }
}
