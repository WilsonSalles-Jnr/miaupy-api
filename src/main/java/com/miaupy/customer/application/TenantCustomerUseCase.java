package com.miaupy.customer.application;

import com.miaupy.customer.domain.*;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.tenant.TenantContext;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantCustomerUseCase {
    private final TenantContext tenants; private final TenantCustomerRepository repository;
    public TenantCustomerUseCase(TenantContext tenants,TenantCustomerRepository repository){this.tenants=tenants;this.repository=repository;}
    @Transactional public TenantCustomer create(Command c){return repository.save(TenantCustomer.create(tenants.getRequiredTenantId(),c.name(),c.email(),c.phone(),c.document(),c.notes()));}
    @Transactional(readOnly=true) public TenantCustomer get(UUID id){return required(id,tenants.getRequiredTenantId());}
    @Transactional(readOnly=true) public Page<TenantCustomer> list(int page,int size){return repository.findAllByTenantId(tenants.getRequiredTenantId(),PageRequest.of(page,Math.min(size,100)));}
    @Transactional public TenantCustomer update(UUID id,Command c){Long tenant=tenants.getRequiredTenantId();return repository.save(required(id,tenant).update(c.name(),c.email(),c.phone(),c.document(),c.notes()));}
    @Transactional public void delete(UUID id){Long tenant=tenants.getRequiredTenantId();repository.save(required(id,tenant).deactivate());}
    private TenantCustomer required(UUID id,Long tenant){return repository.findByIdAndTenantId(id,tenant).orElseThrow(()->new ResourceNotFoundException("Customer not found"));}
    public record Command(String name,String email,String phone,String document,String notes){}
}
