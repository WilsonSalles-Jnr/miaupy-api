package com.miaupy.cart.application;

import com.miaupy.business.domain.Business;
import com.miaupy.business.domain.BusinessConfigurationRepository;
import com.miaupy.business.domain.BusinessRepository;
import com.miaupy.cart.domain.Cart;
import com.miaupy.cart.domain.CartRepository;
import com.miaupy.catalog.domain.Product;
import com.miaupy.catalog.domain.ProductRepository;
import com.miaupy.consumer.application.ConsumerProfileUseCase;
import com.miaupy.consumer.domain.ConsumerProfile;
import com.miaupy.shared.exception.ConflictException;
import com.miaupy.shared.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartUseCase {
  private final ConsumerProfileUseCase profiles;
  private final BusinessRepository businesses;
  private final BusinessConfigurationRepository configurations;
  private final ProductRepository products;
  private final CartRepository carts;

  public CartUseCase(
      ConsumerProfileUseCase profiles,
      BusinessRepository businesses,
      BusinessConfigurationRepository configurations,
      ProductRepository products,
      CartRepository carts) {
    this.profiles = profiles;
    this.businesses = businesses;
    this.configurations = configurations;
    this.products = products;
    this.carts = carts;
  }

  @Transactional
  public Optional<Cart> get() {
    return carts.findActiveByConsumerProfileId(profile().id());
  }

  @Transactional
  public Cart addItem(String storeSlug, UUID productId, int quantity) {
    ConsumerProfile profile = profile();
    Business business = publicStore(storeSlug);
    ensureOnlineSales(business.tenantId());
    Product product = availableProduct(productId, business.tenantId());
    if (quantity > product.stockQuantity()) {
      throw new IllegalArgumentException("Requested quantity exceeds available stock");
    }
    Cart cart =
        carts
            .findActiveByConsumerProfileId(profile.id())
            .orElseGet(() -> Cart.create(profile.id(), business.tenantId()));
    if (!cart.tenantId().equals(business.tenantId())) {
      throw new ConflictException(
          "The active cart belongs to another store; remove its items or complete checkout first");
    }
    Cart changed = cart.add(product.id(), quantity, product.sellingPrice());
    int resultingQuantity =
        changed.items().stream()
            .filter(item -> item.productId().equals(product.id()))
            .findFirst()
            .orElseThrow()
            .quantity();
    if (resultingQuantity > product.stockQuantity()) {
      throw new IllegalArgumentException("Requested quantity exceeds available stock");
    }
    return carts.save(changed);
  }

  @Transactional
  public Cart updateItem(UUID itemId, int quantity) {
    Cart cart = requiredActiveCart();
    UUID productId =
        cart.items().stream()
            .filter(item -> item.id().equals(itemId))
            .map(com.miaupy.cart.domain.CartItem::productId)
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
    Product product = availableProduct(productId, cart.tenantId());
    if (quantity > product.stockQuantity()) {
      throw new IllegalArgumentException("Requested quantity exceeds available stock");
    }
    return carts.save(cart.updateItem(itemId, quantity, product.sellingPrice()));
  }

  @Transactional
  public Cart removeItem(UUID itemId) {
    try {
      return carts.save(requiredActiveCart().removeItem(itemId));
    } catch (IllegalArgumentException exception) {
      throw new ResourceNotFoundException("Cart item not found");
    }
  }

  private Cart requiredActiveCart() {
    return carts
        .findActiveByConsumerProfileId(profile().id())
        .orElseThrow(() -> new ResourceNotFoundException("Active cart not found"));
  }

  private ConsumerProfile profile() {
    return profiles.getMe();
  }

  private Business publicStore(String slug) {
    return businesses
        .findPublicBySlug(slug.strip().toLowerCase())
        .orElseThrow(() -> new ResourceNotFoundException("Public store not found"));
  }

  private Product availableProduct(UUID productId, Long tenantId) {
    return products
        .findPublishedByIdAndTenantId(productId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Published product not found"));
  }

  private void ensureOnlineSales(Long tenantId) {
    boolean enabled =
        configurations
            .findSettingsByTenantId(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Business settings not found"))
            .allowOnlineSales();
    if (!enabled) throw new IllegalArgumentException("Online sales are disabled for this store");
  }
}
