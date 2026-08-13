--liquibase formatted sql

--changeset miaupy:009-consumer-store-links
CREATE UNIQUE INDEX uk_customer_tenant_consumer_active
    ON crm.tenant_customer (tenant_id, consumer_profile_id)
    WHERE consumer_profile_id IS NOT NULL AND active = TRUE;

CREATE UNIQUE INDEX uk_pet_tenant_consumer_active
    ON pet.tenant_pet (tenant_id, consumer_pet_id)
    WHERE consumer_pet_id IS NOT NULL AND active = TRUE;
