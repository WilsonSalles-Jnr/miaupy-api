--liquibase formatted sql

--changeset miaupy:011-create-product
CREATE TABLE catalog.product (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    sku VARCHAR(80),
    name VARCHAR(180) NOT NULL,
    description VARCHAR(3000),
    price NUMERIC(19,2) NOT NULL,
    promotional_price NUMERIC(19,2),
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_product_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_product_business FOREIGN KEY (tenant_id) REFERENCES platform.business (tenant_id),
    CONSTRAINT ck_product_price CHECK (price > 0),
    CONSTRAINT ck_product_promotional_price CHECK (
        promotional_price IS NULL OR (promotional_price > 0 AND promotional_price <= price)
    ),
    CONSTRAINT ck_product_stock CHECK (stock_quantity >= 0),
    CONSTRAINT ck_product_published_active CHECK (published = FALSE OR active = TRUE)
);
CREATE INDEX idx_product_tenant_active ON catalog.product (tenant_id, active);
CREATE INDEX idx_product_tenant_created ON catalog.product (tenant_id, created_at DESC);
CREATE INDEX idx_product_public ON catalog.product (tenant_id, published, active, created_at DESC);
CREATE UNIQUE INDEX uk_product_tenant_sku_active ON catalog.product (tenant_id, sku)
    WHERE sku IS NOT NULL AND active = TRUE;

--changeset miaupy:012-create-service
CREATE TABLE catalog.service (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(180) NOT NULL,
    description VARCHAR(3000),
    duration_minutes INTEGER NOT NULL,
    price NUMERIC(19,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    requires_approval BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_service_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_service_business FOREIGN KEY (tenant_id) REFERENCES platform.business (tenant_id),
    CONSTRAINT ck_service_duration CHECK (duration_minutes > 0),
    CONSTRAINT ck_service_price CHECK (price > 0),
    CONSTRAINT ck_service_published_active CHECK (published = FALSE OR active = TRUE)
);
CREATE INDEX idx_service_tenant_active ON catalog.service (tenant_id, active);
CREATE INDEX idx_service_tenant_created ON catalog.service (tenant_id, created_at DESC);
CREATE INDEX idx_service_public ON catalog.service (tenant_id, published, active, created_at DESC);
