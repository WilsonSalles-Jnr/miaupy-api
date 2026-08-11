package com.miaupy.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miaupy.business.domain.Business;
import com.miaupy.business.domain.BusinessRepository;
import com.miaupy.catalog.application.PublicCatalogModels.ProductPage;
import com.miaupy.catalog.domain.OfferedServiceRepository;
import com.miaupy.catalog.domain.Product;
import com.miaupy.catalog.domain.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class PublicCatalogUseCaseTest {
  @Test
  void cacheMissFallsBackToPublishedProductsInPostgresqlAndFillsCache() {
    BusinessRepository businesses = mock(BusinessRepository.class);
    ProductRepository products = mock(ProductRepository.class);
    PublicCatalogCache cache = mock(PublicCatalogCache.class);
    Business business = Business.create(101L, "store", "Store", null, null, null, null, null, null);
    business.update("store", "Store", null, null, null, null, null, null, true);
    Product product =
        Product.create(101L, null, "Food", null, new BigDecimal("10.00"), null, 5).publish();
    when(businesses.findPublicBySlug("store")).thenReturn(Optional.of(business));
    when(cache.getProducts(101L, 0, 20)).thenReturn(Optional.empty());
    when(products.findPublishedByTenantId(
            eq(101L), org.mockito.ArgumentMatchers.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(product)));
    PublicCatalogUseCase useCase =
        new PublicCatalogUseCase(businesses, products, mock(OfferedServiceRepository.class), cache);

    ProductPage page = useCase.products("store", 0, 20);

    assertThat(page.content()).extracting("name").containsExactly("Food");
    ArgumentCaptor<ProductPage> cached = ArgumentCaptor.forClass(ProductPage.class);
    verify(cache).putProducts(eq(101L), cached.capture());
    assertThat(cached.getValue().content()).hasSize(1);
  }
}
