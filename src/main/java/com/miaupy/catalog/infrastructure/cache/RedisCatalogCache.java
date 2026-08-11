package com.miaupy.catalog.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miaupy.catalog.application.CatalogCacheInvalidator;
import com.miaupy.catalog.application.PublicCatalogCache;
import com.miaupy.catalog.application.PublicCatalogModels.ProductPage;
import com.miaupy.catalog.application.PublicCatalogModels.ServicePage;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
class RedisCatalogCache implements PublicCatalogCache, CatalogCacheInvalidator {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisCatalogCache.class);
  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;
  private final Duration ttl;

  RedisCatalogCache(
      StringRedisTemplate redis,
      ObjectMapper objectMapper,
      @Value("${miaupy.cache.public-catalog-ttl:PT5M}") Duration ttl) {
    this.redis = redis;
    this.objectMapper = objectMapper;
    this.ttl = ttl;
  }

  public Optional<ProductPage> getProducts(Long tenantId, int page, int size) {
    return read(pageKey(tenantId, "products", page, size), ProductPage.class);
  }

  public void putProducts(Long tenantId, ProductPage value) {
    write(pageKey(tenantId, "products", value.page(), value.size()), value);
  }

  public Optional<ServicePage> getServices(Long tenantId, int page, int size) {
    return read(pageKey(tenantId, "services", page, size), ServicePage.class);
  }

  public void putServices(Long tenantId, ServicePage value) {
    write(pageKey(tenantId, "services", value.page(), value.size()), value);
  }

  public void invalidateProducts(Long tenantId) {
    incrementVersion(tenantId, "products");
  }

  public void invalidateServices(Long tenantId) {
    incrementVersion(tenantId, "services");
  }

  private <T> Optional<T> read(String key, Class<T> type) {
    try {
      String json = redis.opsForValue().get(key);
      return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, type));
    } catch (RuntimeException | java.io.IOException exception) {
      LOGGER.warn("Public catalog cache read failed; falling back to PostgreSQL");
      return Optional.empty();
    }
  }

  private void write(String key, Object value) {
    try {
      redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
    } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
      LOGGER.warn("Public catalog cache write failed; PostgreSQL remains authoritative");
    }
  }

  private void incrementVersion(Long tenantId, String catalog) {
    try {
      redis.opsForValue().increment(versionKey(tenantId, catalog));
    } catch (RuntimeException exception) {
      LOGGER.warn("Public catalog cache invalidation failed; cached entries will expire by TTL");
    }
  }

  private String pageKey(Long tenantId, String catalog, int page, int size) {
    return "store:%d:%s:v%s:p%d:s%d"
        .formatted(tenantId, catalog, version(tenantId, catalog), page, size);
  }

  private String version(Long tenantId, String catalog) {
    try {
      return Optional.ofNullable(redis.opsForValue().get(versionKey(tenantId, catalog)))
          .orElse("0");
    } catch (RuntimeException exception) {
      return "0";
    }
  }

  private String versionKey(Long tenantId, String catalog) {
    return "store:%d:%s:version".formatted(tenantId, catalog);
  }
}
