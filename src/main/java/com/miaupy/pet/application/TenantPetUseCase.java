package com.miaupy.pet.application;

import com.miaupy.customer.domain.TenantCustomerRepository;
import com.miaupy.pet.domain.*;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantPetUseCase {
    private final TenantContext tenants;private final TenantCustomerRepository customers;private final TenantPetRepository pets;
    public TenantPetUseCase(TenantContext tenants,TenantCustomerRepository customers,TenantPetRepository pets){this.tenants=tenants;this.customers=customers;this.pets=pets;}
    @Transactional public TenantPet create(UUID customerId,Command c){Long tenant=tenants.getRequiredTenantId();requireCustomer(customerId,tenant);return pets.save(TenantPet.create(tenant,customerId,c.name(),c.species(),c.breed(),c.birthDate(),c.sex(),c.weight(),c.notes()));}
    @Transactional(readOnly=true) public Page<TenantPet> list(UUID customerId,int page,int size){Long tenant=tenants.getRequiredTenantId();requireCustomer(customerId,tenant);return pets.findAllByCustomerIdAndTenantId(customerId,tenant,PageRequest.of(page,Math.min(size,100)));}
    @Transactional(readOnly=true) public TenantPet get(UUID id){return required(id,tenants.getRequiredTenantId());}
    @Transactional public TenantPet update(UUID id,Command c){Long tenant=tenants.getRequiredTenantId();return pets.save(required(id,tenant).update(c.name(),c.species(),c.breed(),c.birthDate(),c.sex(),c.weight(),c.notes()));}
    @Transactional public void delete(UUID id){Long tenant=tenants.getRequiredTenantId();pets.save(required(id,tenant).deactivate());}
    private void requireCustomer(UUID id,Long tenant){customers.findByIdAndTenantId(id,tenant).orElseThrow(()->new ResourceNotFoundException("Customer not found"));}
    private TenantPet required(UUID id,Long tenant){return pets.findByIdAndTenantId(id,tenant).orElseThrow(()->new ResourceNotFoundException("Pet not found"));}
    public record Command(String name,String species,String breed,LocalDate birthDate,PetSex sex,BigDecimal weight,String notes){}
}
