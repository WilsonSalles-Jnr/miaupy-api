--liquibase formatted sql

--changeset miaupy:110-create-employee
CREATE TABLE platform.employee (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    auth_subject VARCHAR(160) NOT NULL,
    name VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(32),
    role VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_employee_auth_subject UNIQUE (auth_subject),
    CONSTRAINT uk_employee_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT fk_employee_tenant FOREIGN KEY (tenant_id)
        REFERENCES platform.tenant (id),
    CONSTRAINT ck_employee_role CHECK (role IN (
        'RECEPTIONIST',
        'VETERINARIAN',
        'GROOMER',
        'CATALOG_MANAGER',
        'SCHEDULING_MANAGER',
        'ORDER_MANAGER'
    ))
);
CREATE INDEX idx_employee_tenant_active_name
    ON platform.employee (tenant_id, active, name);

