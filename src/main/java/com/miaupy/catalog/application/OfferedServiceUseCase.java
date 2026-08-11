package com.miaupy.catalog.application;

import com.miaupy.catalog.domain.OfferedService;
import com.miaupy.catalog.domain.OfferedServiceRepository;
import com.miaupy.integration.outbox.OutboxWriter;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.tenant.TenantContext;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfferedServiceUseCase {
  private final TenantContext tenantContext;
  private final OfferedServiceRepository repository;
  private final OutboxWriter outbox;
  private final CatalogCacheInvalidator cache;

  public OfferedServiceUseCase(
      TenantContext tenantContext,
      OfferedServiceRepository repository,
      OutboxWriter outbox,
      CatalogCacheInvalidator cache) {
    this.tenantContext = tenantContext;
    this.repository = repository;
    this.outbox = outbox;
    this.cache = cache;
  }

  @Transactional
  public OfferedService create(Command c) {
    Long tenantId = tenantContext.getRequiredTenantId();
    OfferedService saved =
        repository.save(
            OfferedService.create(
                tenantId,
                c.name(),
                c.description(),
                c.durationMinutes(),
                c.price(),
                c.requiresApproval()));
    append(saved, "service.created");
    return saved;
  }

  @Transactional(readOnly = true)
  public OfferedService get(UUID id) {
    return required(id, tenantContext.getRequiredTenantId());
  }

  @Transactional(readOnly = true)
  public Page<OfferedService> list(int page, int size) {
    return repository.findAllByTenantId(
        tenantContext.getRequiredTenantId(),
        PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
  }

  @Transactional
  public OfferedService update(UUID id, Command c) {
    Long tenantId = tenantContext.getRequiredTenantId();
    OfferedService saved =
        repository.save(
            required(id, tenantId)
                .update(
                    c.name(),
                    c.description(),
                    c.durationMinutes(),
                    c.price(),
                    c.requiresApproval()));
    append(saved, "service.updated");
    cache.invalidateServices(tenantId);
    return saved;
  }

  @Transactional
  public OfferedService publish(UUID id) {
    return publication(id, true);
  }

  @Transactional
  public OfferedService unpublish(UUID id) {
    return publication(id, false);
  }

  @Transactional
  public void delete(UUID id) {
    Long tenantId = tenantContext.getRequiredTenantId();
    OfferedService saved = repository.save(required(id, tenantId).deactivate());
    append(saved, "service.updated");
    cache.invalidateServices(tenantId);
  }

  private OfferedService publication(UUID id, boolean publish) {
    Long tenantId = tenantContext.getRequiredTenantId();
    OfferedService current = required(id, tenantId);
    OfferedService saved = repository.save(publish ? current.publish() : current.unpublish());
    append(saved, publish ? "service.published" : "service.unpublished");
    cache.invalidateServices(tenantId);
    return saved;
  }

  private OfferedService required(UUID id, Long tenantId) {
    return repository
        .findByIdAndTenantId(id, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
  }

  private void append(OfferedService service, String eventType) {
    outbox.append(
        "Service",
        service.id(),
        eventType,
        service.tenantId(),
        Map.of(
            "serviceId", service.id(),
            "name", service.name(),
            "active", service.active(),
            "published", service.published()));
  }

  public record Command(
      String name,
      String description,
      int durationMinutes,
      BigDecimal price,
      boolean requiresApproval) {}
}
