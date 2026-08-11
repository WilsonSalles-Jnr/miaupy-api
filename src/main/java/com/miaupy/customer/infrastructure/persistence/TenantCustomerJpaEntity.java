package com.miaupy.customer.infrastructure.persistence;

import com.miaupy.customer.domain.TenantCustomer;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="tenant_customer",schema="crm")
class TenantCustomerJpaEntity {
    @Id UUID id;
    @Column(name="tenant_id",nullable=false) Long tenantId;
    @Column(name="consumer_profile_id") UUID consumerProfileId;
    @Column(nullable=false,length=160) String name;
    @Column(length=254) String email;
    @Column(length=32) String phone;
    @Column(length=32) String document;
    @Column(length=2000) String notes;
    @Column(nullable=false) boolean active;
    @Column(name="deleted_at") Instant deletedAt;
    @Column(name="created_at",nullable=false,updatable=false) Instant createdAt;
    @Column(name="updated_at",nullable=false) Instant updatedAt;
    @Version Long version;
    protected TenantCustomerJpaEntity(){}
    TenantCustomerJpaEntity(TenantCustomer c){id=c.id();tenantId=c.tenantId();consumerProfileId=c.consumerProfileId();name=c.name();email=c.email();phone=c.phone();document=c.document();notes=c.notes();active=c.active();deletedAt=c.deletedAt();createdAt=c.createdAt();updatedAt=c.updatedAt();version=c.version();}
}
