--liquibase formatted sql

--changeset miaupy:060-create-cart
CREATE TABLE sales.cart (
    id UUID PRIMARY KEY,
    consumer_profile_id UUID NOT NULL,
    tenant_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_cart_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT fk_cart_consumer FOREIGN KEY (consumer_profile_id) REFERENCES consumer.consumer_profile (id),
    CONSTRAINT fk_cart_business FOREIGN KEY (tenant_id) REFERENCES platform.business (tenant_id),
    CONSTRAINT ck_cart_status CHECK (status IN ('ACTIVE','CHECKED_OUT','ABANDONED'))
);
CREATE UNIQUE INDEX uk_cart_consumer_active ON sales.cart (consumer_profile_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_cart_consumer_updated ON sales.cart (consumer_profile_id, updated_at DESC);
CREATE INDEX idx_cart_tenant_status ON sales.cart (tenant_id, status, updated_at DESC);

--changeset miaupy:061-create-cart-item
CREATE TABLE sales.cart_item (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL,
    tenant_id BIGINT NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19,2) NOT NULL,
    CONSTRAINT uk_cart_item_product UNIQUE (cart_id, product_id),
    CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id, tenant_id) REFERENCES sales.cart (id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_product FOREIGN KEY (product_id, tenant_id) REFERENCES catalog.product (id, tenant_id),
    CONSTRAINT ck_cart_item_quantity CHECK (quantity > 0),
    CONSTRAINT ck_cart_item_price CHECK (unit_price > 0)
);
CREATE INDEX idx_cart_item_cart ON sales.cart_item (cart_id);

--changeset miaupy:062-create-customer-order
CREATE TABLE sales.customer_order (
    id UUID PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    consumer_profile_id UUID NOT NULL,
    tenant_customer_id UUID,
    checkout_key UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    subtotal NUMERIC(19,2) NOT NULL,
    discount NUMERIC(19,2) NOT NULL,
    total NUMERIC(19,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_order_id_tenant UNIQUE (id, tenant_id),
    CONSTRAINT uk_order_consumer_checkout UNIQUE (consumer_profile_id, checkout_key),
    CONSTRAINT fk_order_business FOREIGN KEY (tenant_id) REFERENCES platform.business (tenant_id),
    CONSTRAINT fk_order_consumer FOREIGN KEY (consumer_profile_id) REFERENCES consumer.consumer_profile (id),
    CONSTRAINT fk_order_customer FOREIGN KEY (tenant_customer_id, tenant_id) REFERENCES crm.tenant_customer (id, tenant_id),
    CONSTRAINT ck_order_status CHECK (status IN ('CREATED','AWAITING_PAYMENT','PAID','PROCESSING','READY','COMPLETED','CANCELLED','REFUNDED')),
    CONSTRAINT ck_order_values CHECK (subtotal >= 0 AND discount >= 0 AND total >= 0 AND total = subtotal - discount)
);
CREATE INDEX idx_order_tenant_created ON sales.customer_order (tenant_id, created_at DESC);
CREATE INDEX idx_order_tenant_status ON sales.customer_order (tenant_id, status, created_at DESC);
CREATE INDEX idx_order_consumer_created ON sales.customer_order (consumer_profile_id, created_at DESC);

--changeset miaupy:063-create-order-item
CREATE TABLE sales.order_item (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    tenant_id BIGINT NOT NULL,
    product_id UUID NOT NULL,
    product_name VARCHAR(180) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(19,2) NOT NULL,
    total NUMERIC(19,2) NOT NULL,
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id, tenant_id) REFERENCES sales.customer_order (id, tenant_id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id, tenant_id) REFERENCES catalog.product (id, tenant_id),
    CONSTRAINT ck_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_item_values CHECK (unit_price > 0 AND total = unit_price * quantity)
);
CREATE INDEX idx_order_item_order ON sales.order_item (order_id);
