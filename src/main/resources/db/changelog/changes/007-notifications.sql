--liquibase formatted sql

--changeset miaupy:070-create-notification
CREATE TABLE integration.notification (
    id UUID PRIMARY KEY,
    tenant_id BIGINT,
    consumer_profile_id UUID,
    source_event_id UUID NOT NULL,
    deduplication_key VARCHAR(255) NOT NULL,
    notification_type VARCHAR(80) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(320) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    body VARCHAR(5000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    last_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_notification_deduplication UNIQUE (deduplication_key),
    CONSTRAINT fk_notification_business FOREIGN KEY (tenant_id) REFERENCES platform.business (tenant_id),
    CONSTRAINT fk_notification_consumer FOREIGN KEY (consumer_profile_id) REFERENCES consumer.consumer_profile (id),
    CONSTRAINT ck_notification_channel CHECK (channel IN ('EMAIL','PUSH','WHATSAPP')),
    CONSTRAINT ck_notification_status CHECK (status IN ('PENDING','SENT','FAILED','SKIPPED')),
    CONSTRAINT ck_notification_attempts CHECK (attempts >= 0)
);
CREATE INDEX idx_notification_pending ON integration.notification (status, next_attempt_at);
CREATE INDEX idx_notification_tenant_created ON integration.notification (tenant_id, created_at DESC);
CREATE INDEX idx_notification_consumer_created
    ON integration.notification (consumer_profile_id, created_at DESC);
