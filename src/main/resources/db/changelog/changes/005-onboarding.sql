--liquibase formatted sql

--changeset miaupy:050-create-tenant-registry
CREATE SEQUENCE platform.tenant_id_seq START WITH 50000001 INCREMENT BY 1;
CREATE TABLE platform.tenant (
    id BIGINT PRIMARY KEY DEFAULT nextval('platform.tenant_id_seq'),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_tenant_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);
INSERT INTO platform.tenant (id)
SELECT DISTINCT tenant_id FROM platform.business
ON CONFLICT (id) DO NOTHING;
SELECT setval(
    'platform.tenant_id_seq',
    GREATEST(50000000, COALESCE((SELECT MAX(id) FROM platform.tenant), 50000000)),
    TRUE
);
--changeset miaupy:051-create-provider-upgrade
CREATE TABLE platform.provider_upgrade (
    id UUID PRIMARY KEY,
    auth_subject VARCHAR(160) NOT NULL,
    consumer_profile_id UUID NOT NULL,
    idempotency_key UUID NOT NULL,
    request_fingerprint CHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL,
    business_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_provider_upgrade_subject UNIQUE (auth_subject),
    CONSTRAINT uk_provider_upgrade_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_provider_upgrade_status CHECK (status IN ('LOCAL_READY', 'COMPLETED')),
    CONSTRAINT fk_provider_upgrade_consumer FOREIGN KEY (consumer_profile_id)
        REFERENCES consumer.consumer_profile (id),
    CONSTRAINT fk_provider_upgrade_tenant FOREIGN KEY (tenant_id)
        REFERENCES platform.tenant (id),
    CONSTRAINT fk_provider_upgrade_business FOREIGN KEY (business_id)
        REFERENCES platform.business (id)
);
CREATE INDEX idx_provider_upgrade_status_updated
    ON platform.provider_upgrade (status, updated_at);
