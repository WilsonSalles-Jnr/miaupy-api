--liquibase formatted sql

--changeset miaupy:100-create-business-registration
CREATE TABLE platform.business_registration (
    id UUID PRIMARY KEY,
    auth_subject VARCHAR(160) NOT NULL,
    idempotency_key UUID NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL,
    business_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_business_registration_subject UNIQUE (auth_subject),
    CONSTRAINT uk_business_registration_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_business_registration_status CHECK (status IN ('LOCAL_READY', 'COMPLETED')),
    CONSTRAINT fk_business_registration_tenant FOREIGN KEY (tenant_id)
        REFERENCES platform.tenant (id),
    CONSTRAINT fk_business_registration_business FOREIGN KEY (business_id)
        REFERENCES platform.business (id)
);
CREATE INDEX idx_business_registration_status_updated
    ON platform.business_registration (status, updated_at);
