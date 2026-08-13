--liquibase formatted sql

--changeset miaupy:080-create-clinical-schema
CREATE SCHEMA IF NOT EXISTS clinical;

--changeset miaupy:081-create-medical-record
CREATE TABLE clinical.medical_record (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_pet_id UUID NOT NULL,
    allergies VARCHAR(3000),
    chronic_conditions VARCHAR(3000),
    current_medications VARCHAR(3000),
    notes VARCHAR(5000),
    created_by VARCHAR(160) NOT NULL,
    updated_by VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_medical_record_pet UNIQUE (tenant_id, tenant_pet_id),
    CONSTRAINT fk_medical_record_pet FOREIGN KEY (tenant_pet_id, tenant_id)
        REFERENCES pet.tenant_pet (id, tenant_id)
);
CREATE INDEX idx_medical_record_tenant ON clinical.medical_record (tenant_id, updated_at DESC);

--changeset miaupy:082-create-clinical-consultation
CREATE TABLE clinical.consultation (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_pet_id UUID NOT NULL,
    appointment_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    anamnesis VARCHAR(5000),
    clinical_findings VARCHAR(5000),
    diagnosis VARCHAR(3000),
    treatment_plan VARCHAR(5000),
    weight NUMERIC(8,2),
    temperature NUMERIC(4,1),
    veterinarian_subject VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_consultation_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uk_consultation_appointment UNIQUE (tenant_id, appointment_id),
    CONSTRAINT fk_consultation_pet FOREIGN KEY (tenant_pet_id, tenant_id)
        REFERENCES pet.tenant_pet (id, tenant_id),
    CONSTRAINT fk_consultation_appointment FOREIGN KEY (appointment_id, tenant_id)
        REFERENCES scheduling.appointment (id, tenant_id),
    CONSTRAINT ck_consultation_weight CHECK (weight IS NULL OR weight > 0),
    CONSTRAINT ck_consultation_temperature CHECK (
        temperature IS NULL OR temperature BETWEEN 20.0 AND 50.0
    )
);
CREATE INDEX idx_consultation_pet_occurred
    ON clinical.consultation (tenant_id, tenant_pet_id, occurred_at DESC);

--changeset miaupy:083-create-vaccination
CREATE TABLE clinical.vaccination (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_pet_id UUID NOT NULL,
    vaccine_name VARCHAR(200) NOT NULL,
    manufacturer VARCHAR(200),
    batch_number VARCHAR(100),
    administered_on DATE NOT NULL,
    next_due_on DATE,
    veterinarian_subject VARCHAR(160) NOT NULL,
    notes VARCHAR(3000),
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_vaccination_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_vaccination_pet FOREIGN KEY (tenant_pet_id, tenant_id)
        REFERENCES pet.tenant_pet (id, tenant_id),
    CONSTRAINT ck_vaccination_due CHECK (next_due_on IS NULL OR next_due_on >= administered_on)
);
CREATE INDEX idx_vaccination_pet_date
    ON clinical.vaccination (tenant_id, tenant_pet_id, administered_on DESC);

--changeset miaupy:084-create-prescription
CREATE TABLE clinical.prescription (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_pet_id UUID NOT NULL,
    consultation_id UUID,
    medication VARCHAR(300) NOT NULL,
    dosage VARCHAR(500) NOT NULL,
    frequency VARCHAR(500) NOT NULL,
    duration VARCHAR(500),
    instructions VARCHAR(3000),
    issued_at TIMESTAMPTZ NOT NULL,
    valid_until DATE,
    veterinarian_subject VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_prescription_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_prescription_pet FOREIGN KEY (tenant_pet_id, tenant_id)
        REFERENCES pet.tenant_pet (id, tenant_id),
    CONSTRAINT fk_prescription_consultation FOREIGN KEY (consultation_id, tenant_id)
        REFERENCES clinical.consultation (id, tenant_id)
);
CREATE INDEX idx_prescription_pet_issued
    ON clinical.prescription (tenant_id, tenant_pet_id, issued_at DESC);

--changeset miaupy:085-create-clinical-attachment
CREATE TABLE clinical.attachment (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_pet_id UUID NOT NULL,
    consultation_id UUID,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    content BYTEA NOT NULL,
    uploaded_by VARCHAR(160) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_attachment_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_attachment_pet FOREIGN KEY (tenant_pet_id, tenant_id)
        REFERENCES pet.tenant_pet (id, tenant_id),
    CONSTRAINT fk_attachment_consultation FOREIGN KEY (consultation_id, tenant_id)
        REFERENCES clinical.consultation (id, tenant_id),
    CONSTRAINT ck_attachment_size CHECK (
        size_bytes > 0 AND size_bytes <= 10485760 AND octet_length(content) = size_bytes
    ),
    CONSTRAINT ck_attachment_type CHECK (content_type IN ('application/pdf','image/jpeg','image/png'))
);
CREATE INDEX idx_attachment_pet_created
    ON clinical.attachment (tenant_id, tenant_pet_id, active, created_at DESC);

--changeset miaupy:086-create-clinical-history
CREATE TABLE clinical.history_event (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_pet_id UUID NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    resource_id UUID NOT NULL,
    summary VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    recorded_by VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_clinical_history_pet FOREIGN KEY (tenant_pet_id, tenant_id)
        REFERENCES pet.tenant_pet (id, tenant_id)
);
CREATE INDEX idx_clinical_history_pet_occurred
    ON clinical.history_event (tenant_id, tenant_pet_id, occurred_at DESC);
