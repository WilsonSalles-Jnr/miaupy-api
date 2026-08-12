package com.miaupy.order.application;

import com.miaupy.business.domain.BusinessConfigurationRepository;
import com.miaupy.cart.domain.Cart;
import com.miaupy.cart.domain.CartItem;
import com.miaupy.cart.domain.CartRepository;
import com.miaupy.catalog.domain.Product;
import com.miaupy.catalog.domain.ProductRepository;
import com.miaupy.consumer.application.ConsumerProfileUseCase;
import com.miaupy.consumer.domain.ConsumerProfile;
import com.miaupy.customer.domain.TenantCustomerRepository;
import com.miaupy.integration.outbox.OutboxWriter;
import com.miaupy.order.domain.CustomerOrder;
import com.miaupy.order.domain.CustomerOrderRepository;
import com.miaupy.order.domain.OrderItem;
import com.miaupy.shared.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutUseCase {
  private final ConsumerProfileUseCase profiles;
  private final CartRepository carts;
  private final CustomerOrderRepository orders;
  private final ProductRepository products;
  private final TenantCustomerRepository customers;
  private final BusinessConfigurationRepository configurations;
  private final OutboxWriter outbox;

  public CheckoutUseCase(
      ConsumerProfileUseCase profiles,
      CartRepository carts,
      CustomerOrderRepository orders,
      ProductRepository products,
      TenantCustomerRepository customers,
      BusinessConfigurationRepository configurations,
      OutboxWriter outbox) {
    this.profiles = profiles;
    this.carts = carts;
    this.orders = orders;
    this.products = products;
    this.customers = customers;
    this.configurations = configurations;
    this.outbox = outbox;
  }

  @Transactional
  public CustomerOrder checkout(UUID idempotencyKey) {
    ConsumerProfile profile = profiles.getMe();
    CustomerOrder existing = existing(profile.id(), idempotencyKey);
    if (existing != null) return existing;
    var lockedCart = carts.lockActiveByConsumerProfileId(profile.id());
    if (lockedCart.isEmpty()) {
      CustomerOrder retried = existing(profile.id(), idempotencyKey);
      if (retried != null) return retried;
      throw new ResourceNotFoundException("Active cart not found");
    }
    Cart cart = lockedCart.get();
    if (cart.items().isEmpty()) throw new IllegalArgumentException("Cart is empty");
    boolean onlineSales =
        configurations
            .findSettingsByTenantId(cart.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("Business settings not found"))
            .allowOnlineSales();
    if (!onlineSales)
      throw new IllegalArgumentException("Online sales are disabled for this store");

    List<CartItem> sorted =
        cart.items().stream().sorted(Comparator.comparing(CartItem::productId)).toList();
    List<OrderItem> snapshots = new ArrayList<>();
    for (CartItem item : sorted) {
      Product product =
          products
              .lockByIdAndTenantId(item.productId(), cart.tenantId())
              .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
      Product reserved = product.reserveStock(item.quantity());
      products.save(reserved);
      snapshots.add(
          OrderItem.snapshot(
              product.id(), product.name(), item.quantity(), product.sellingPrice()));
    }
    UUID tenantCustomerId =
        customers
            .findByConsumerProfileIdAndTenantId(profile.id(), cart.tenantId())
            .map(customer -> customer.id())
            .orElse(null);
    CustomerOrder order =
        orders.save(
            CustomerOrder.create(
                cart.tenantId(), profile.id(), tenantCustomerId, idempotencyKey, snapshots));
    carts.save(cart.checkedOut());
    outbox.append(
        "Cart",
        cart.id(),
        "cart.checked-out",
        cart.tenantId(),
        Map.of("cartId", cart.id(), "orderId", order.id()));
    outbox.append(
        "CustomerOrder",
        order.id(),
        "order.created",
        order.tenantId(),
        Map.of("orderId", order.id(), "status", order.status(), "total", order.total()));
    return order;
  }

  private CustomerOrder existing(UUID consumerProfileId, UUID key) {
    return orders.findByConsumerProfileIdAndCheckoutKey(consumerProfileId, key).orElse(null);
  }
}
