package com.miaupy.business.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "business", schema = "platform")
class BusinessJpaEntity {

    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false, unique = true)
    private Long tenantId;
    @Column(nullable = false, unique = true, length = 80)
    private String slug;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(name = "trade_name", length = 160)
    private String tradeName;
    @Column(length = 32)
    private String document;
    @Column(length = 2000)
    private String description;
    @Column(length = 32)
    private String phone;
    @Column(length = 254)
    private String email;
    @Column(length = 500)
    private String website;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "public_visible", nullable = false)
    private boolean publicVisible;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private Long version;

    protected BusinessJpaEntity() {
    }

    BusinessJpaEntity(UUID id, Long tenantId, String slug, String name, String tradeName, String document,
                      String description, String phone, String email, String website, boolean active,
                      boolean publicVisible, Instant createdAt, Instant updatedAt, Long version) {
        this.id = id;
        this.tenantId = tenantId;
        this.slug = slug;
        this.name = name;
        this.tradeName = tradeName;
        this.document = document;
        this.description = description;
        this.phone = phone;
        this.email = email;
        this.website = website;
        this.active = active;
        this.publicVisible = publicVisible;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    UUID getId() { return id; }
    Long getTenantId() { return tenantId; }
    String getSlug() { return slug; }
    String getName() { return name; }
    String getTradeName() { return tradeName; }
    String getDocument() { return document; }
    String getDescription() { return description; }
    String getPhone() { return phone; }
    String getEmail() { return email; }
    String getWebsite() { return website; }
    boolean isActive() { return active; }
    boolean isPublicVisible() { return publicVisible; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }
    Long getVersion() { return version; }
}
