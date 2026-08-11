--liquibase formatted sql

--changeset miaupy:013-enable-btree-gist
CREATE EXTENSION IF NOT EXISTS btree_gist;

--changeset miaupy:014-create-availability-rule
CREATE TABLE scheduling.availability_rule (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    employee_id UUID,
    day_of_week SMALLINT NOT NULL,
    start_local TIME NOT NULL,
    end_local TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_availability_business FOREIGN KEY (tenant_id) REFERENCES platform.business (tenant_id),
    CONSTRAINT ck_availability_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_availability_interval CHECK (end_local > start_local)
);
CREATE INDEX idx_availability_tenant_day
    ON scheduling.availability_rule (tenant_id, day_of_week, active);

--changeset miaupy:015-create-appointment
CREATE TABLE scheduling.appointment (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_customer_id UUID NOT NULL,
    tenant_pet_id UUID NOT NULL,
    service_id UUID NOT NULL,
    employee_id UUID,
    schedule_resource VARCHAR(80) NOT NULL,
    requested_by VARCHAR(20) NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL,
    notes VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_appointment_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_appointment_customer FOREIGN KEY (tenant_customer_id, tenant_id)
        REFERENCES crm.tenant_customer (id, tenant_id),
    CONSTRAINT fk_appointment_pet FOREIGN KEY (tenant_pet_id, tenant_id)
        REFERENCES pet.tenant_pet (id, tenant_id),
    CONSTRAINT fk_appointment_service FOREIGN KEY (service_id, tenant_id)
        REFERENCES catalog.service (id, tenant_id),
    CONSTRAINT ck_appointment_interval CHECK (end_at > start_at),
    CONSTRAINT ck_appointment_origin CHECK (requested_by IN ('CUSTOMER','BUSINESS')),
    CONSTRAINT ck_appointment_status CHECK (
        status IN ('REQUESTED','CONFIRMED','REJECTED','CANCELLED','IN_PROGRESS','COMPLETED','NO_SHOW')
    )
);
ALTER TABLE scheduling.appointment
    ADD CONSTRAINT ex_appointment_occupied_slot
    EXCLUDE USING gist (
        tenant_id WITH =,
        schedule_resource WITH =,
        tstzrange(start_at, end_at, '[)') WITH &&
    ) WHERE (status IN ('REQUESTED','CONFIRMED','IN_PROGRESS'));
CREATE INDEX idx_appointment_tenant_start ON scheduling.appointment (tenant_id, start_at);
CREATE INDEX idx_appointment_tenant_status ON scheduling.appointment (tenant_id, status, start_at);
CREATE INDEX idx_appointment_customer_created
    ON scheduling.appointment (tenant_customer_id, created_at DESC);

--changeset miaupy:016-outbox-publisher-columns
ALTER TABLE integration.domain_event_outbox ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE integration.domain_event_outbox ADD COLUMN last_error VARCHAR(1000);
