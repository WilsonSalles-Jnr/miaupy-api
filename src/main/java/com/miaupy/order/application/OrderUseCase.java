package com.miaupy.order.application;

import com.miaupy.catalog.domain.Product;
import com.miaupy.catalog.domain.ProductRepository;
import com.miaupy.consumer.application.ConsumerProfileUseCase;
import com.miaupy.integration.outbox.OutboxWriter;
import com.miaupy.order.domain.CustomerOrder;
import com.miaupy.order.domain.CustomerOrderRepository;
import com.miaupy.order.domain.OrderItem;
import com.miaupy.order.domain.OrderStatus;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.tenant.TenantContext;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderUseCase {
  private final TenantContext tenantContext;
  private final ConsumerProfileUseCase profiles;
  private final CustomerOrderRepository orders;
  private final ProductRepository products;
  private final OutboxWriter outbox;

  public OrderUseCase(
      TenantContext tenantContext,
      ConsumerProfileUseCase profiles,
      CustomerOrderRepository orders,
      ProductRepository products,
      OutboxWriter outbox) {
    this.tenantContext = tenantContext;
    this.profiles = profiles;
    this.orders = orders;
    this.products = products;
    this.outbox = outbox;
  }

  @Transactional
  public Page<CustomerOrder> listConsumer(int page, int size) {
    return orders.findAllByConsumerProfileId(profiles.getMe().id(), page(page, size));
  }

  @Transactional
  public CustomerOrder getConsumer(UUID id) {
    return orders
        .findByIdAndConsumerProfileId(id, profiles.getMe().id())
        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
  }

  @Transactional(readOnly = true)
  public Page<CustomerOrder> listBusiness(int page, int size) {
    return orders.findAllByTenantId(tenantContext.getRequiredTenantId(), page(page, size));
  }

  @Transactional(readOnly = true)
  public CustomerOrder getBusiness(UUID id) {
    return orders
        .findByIdAndTenantId(id, tenantContext.getRequiredTenantId())
        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
  }

  @Transactional
  public CustomerOrder transitionBusiness(UUID id, OrderStatus target) {
    Long tenantId = tenantContext.getRequiredTenantId();
    CustomerOrder current =
        orders
            .lockByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    CustomerOrder changed = current.transitionTo(target);
    if (target == OrderStatus.CANCELLED) restoreStock(current);
    CustomerOrder saved = orders.save(changed);
    outbox.append(
        "CustomerOrder",
        saved.id(),
        "order." + target.name().toLowerCase().replace('_', '-'),
        tenantId,
        Map.of("orderId", saved.id(), "status", saved.status(), "total", saved.total()));
    return saved;
  }

  private void restoreStock(CustomerOrder order) {
    order.items().stream()
        .sorted(Comparator.comparing(OrderItem::productId))
        .forEach(
            item -> {
              Product product =
                  products
                      .lockByIdAndTenantId(item.productId(), order.tenantId())
                      .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
              products.save(product.restoreStock(item.quantity()));
            });
  }

  private PageRequest page(int page, int size) {
    return PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
  }
}
