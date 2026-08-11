--liquibase formatted sql

--changeset miaupy:007-create-consumer-profile
CREATE TABLE consumer.consumer_profile (
    id UUID PRIMARY KEY,
    auth_subject VARCHAR(160) NOT NULL,
    name VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(32),
    document VARCHAR(32),
    birth_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_consumer_profile_subject UNIQUE (auth_subject)
);
CREATE INDEX idx_consumer_profile_email ON consumer.consumer_profile (email);

--changeset miaupy:008-create-consumer-pet
CREATE TABLE consumer.consumer_pet (
    id UUID PRIMARY KEY,
    consumer_profile_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    species VARCHAR(60) NOT NULL,
    breed VARCHAR(120),
    birth_date DATE,
    sex VARCHAR(20),
    weight NUMERIC(8,2),
    color VARCHAR(80),
    microchip VARCHAR(80),
    neutered BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_consumer_pet_owner FOREIGN KEY (consumer_profile_id) REFERENCES consumer.consumer_profile (id),
    CONSTRAINT ck_consumer_pet_sex CHECK (sex IS NULL OR sex IN ('MALE','FEMALE','UNKNOWN')),
    CONSTRAINT ck_consumer_pet_weight CHECK (weight IS NULL OR weight > 0)
);
CREATE INDEX idx_consumer_pet_owner_active ON consumer.consumer_pet (consumer_profile_id, active);

--changeset miaupy:009-create-tenant-customer
CREATE TABLE crm.tenant_customer (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    consumer_profile_id UUID,
    name VARCHAR(160) NOT NULL,
    email VARCHAR(254),
    phone VARCHAR(32),
    document VARCHAR(32),
    notes VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_tenant_customer_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_tenant_customer_business FOREIGN KEY (tenant_id) REFERENCES platform.business (tenant_id),
    CONSTRAINT fk_tenant_customer_consumer FOREIGN KEY (consumer_profile_id) REFERENCES consumer.consumer_profile (id)
);
CREATE INDEX idx_customer_tenant_active ON crm.tenant_customer (tenant_id, active);
CREATE INDEX idx_customer_tenant_created ON crm.tenant_customer (tenant_id, created_at DESC);
CREATE INDEX idx_customer_consumer_profile ON crm.tenant_customer (consumer_profile_id) WHERE consumer_profile_id IS NOT NULL;
CREATE UNIQUE INDEX uk_customer_tenant_document_active ON crm.tenant_customer (tenant_id, document)
    WHERE document IS NOT NULL AND active = TRUE;

--changeset miaupy:010-create-tenant-pet
CREATE TABLE pet.tenant_pet (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_customer_id UUID NOT NULL,
    consumer_pet_id UUID,
    name VARCHAR(120) NOT NULL,
    species VARCHAR(60) NOT NULL,
    breed VARCHAR(120),
    birth_date DATE,
    sex VARCHAR(20),
    weight NUMERIC(8,2),
    notes VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_tenant_pet_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_tenant_pet_customer FOREIGN KEY (tenant_customer_id, tenant_id)
        REFERENCES crm.tenant_customer (id, tenant_id),
    CONSTRAINT fk_tenant_pet_consumer_pet FOREIGN KEY (consumer_pet_id) REFERENCES consumer.consumer_pet (id),
    CONSTRAINT ck_tenant_pet_sex CHECK (sex IS NULL OR sex IN ('MALE','FEMALE','UNKNOWN')),
    CONSTRAINT ck_tenant_pet_weight CHECK (weight IS NULL OR weight > 0)
);
CREATE INDEX idx_tenant_pet_customer_active ON pet.tenant_pet (tenant_id, tenant_customer_id, active);
CREATE INDEX idx_tenant_pet_consumer_pet ON pet.tenant_pet (consumer_pet_id) WHERE consumer_pet_id IS NOT NULL;
