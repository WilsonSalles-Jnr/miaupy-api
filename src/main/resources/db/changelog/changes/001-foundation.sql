--liquibase formatted sql

--changeset miaupy:001-create-domain-schemas
CREATE SCHEMA IF NOT EXISTS platform;
CREATE SCHEMA IF NOT EXISTS consumer;
CREATE SCHEMA IF NOT EXISTS crm;
CREATE SCHEMA IF NOT EXISTS pet;
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS scheduling;
CREATE SCHEMA IF NOT EXISTS sales;
CREATE SCHEMA IF NOT EXISTS integration;
CREATE SCHEMA IF NOT EXISTS audit;

--changeset miaupy:002-create-business
CREATE TABLE platform.business (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    slug VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    trade_name VARCHAR(160),
    document VARCHAR(32),
    description VARCHAR(2000),
    phone VARCHAR(32),
    email VARCHAR(254),
    website VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    public_visible BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_business_tenant UNIQUE (tenant_id),
    CONSTRAINT uk_business_slug UNIQUE (slug),
    CONSTRAINT ck_business_tenant_positive CHECK (tenant_id > 0)
);
CREATE INDEX idx_business_public ON platform.business (public_visible, active, slug);

--changeset miaupy:003-create-business-settings
CREATE TABLE platform.business_settings (
    tenant_id BIGINT PRIMARY KEY,
    appointment_approval_mode VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    timezone VARCHAR(64) NOT NULL DEFAULT 'America/Sao_Paulo',
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    allow_online_booking BOOLEAN NOT NULL DEFAULT FALSE,
    allow_online_sales BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_business_settings_tenant FOREIGN KEY (tenant_id)
        REFERENCES platform.business (tenant_id)
);

--changeset miaupy:004-create-business-address
CREATE TABLE platform.business_address (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    business_id UUID NOT NULL,
    street VARCHAR(160) NOT NULL,
    number VARCHAR(30),
    district VARCHAR(100),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(80) NOT NULL,
    postal_code VARCHAR(20),
    latitude NUMERIC(10,7),
    longitude NUMERIC(10,7),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_business_address_business UNIQUE (business_id),
    CONSTRAINT fk_business_address_business FOREIGN KEY (business_id)
        REFERENCES platform.business (id),
    CONSTRAINT fk_business_address_tenant FOREIGN KEY (tenant_id)
        REFERENCES platform.business (tenant_id)
);
CREATE INDEX idx_business_address_tenant ON platform.business_address (tenant_id);

--changeset miaupy:005-create-outbox
CREATE TABLE integration.domain_event_outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    event_version INTEGER NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    tenant_id BIGINT,
    actor_type VARCHAR(20) NOT NULL,
    actor_id VARCHAR(160) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'))
);
CREATE INDEX idx_outbox_pending ON integration.domain_event_outbox (status, created_at);
CREATE INDEX idx_outbox_tenant_created ON integration.domain_event_outbox (tenant_id, created_at);

--changeset miaupy:006-create-processed-event
CREATE TABLE integration.processed_event (
    consumer_name VARCHAR(150) NOT NULL,
    event_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (consumer_name, event_id)
);
