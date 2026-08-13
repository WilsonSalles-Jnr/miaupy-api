package com.miaupy.scheduling.application;

import com.miaupy.consumer.domain.ConsumerPetRepository;
import com.miaupy.consumer.domain.ConsumerProfile;
import com.miaupy.customer.domain.TenantCustomer;
import com.miaupy.customer.domain.TenantCustomerRepository;
import com.miaupy.integration.outbox.OutboxWriter;
import com.miaupy.pet.domain.TenantPet;
import com.miaupy.pet.domain.TenantPetRepository;
import com.miaupy.shared.exception.ResourceNotFoundException;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsumerStoreLinkUseCase {
  private final ConsumerPetRepository consumerPets;
  private final TenantCustomerRepository customers;
  private final TenantPetRepository tenantPets;
  private final OutboxWriter outbox;

  public ConsumerStoreLinkUseCase(ConsumerPetRepository consumerPets,
      TenantCustomerRepository customers, TenantPetRepository tenantPets, OutboxWriter outbox) {
    this.consumerPets = consumerPets;
    this.customers = customers;
    this.tenantPets = tenantPets;
    this.outbox = outbox;
  }

  @Transactional
  public LinkedCustomerPet link(ConsumerProfile profile, UUID consumerPetId, Long tenantId) {
    var consumerPet = consumerPets.findByIdAndOwnerId(consumerPetId, profile.id())
        .orElseThrow(() -> new ResourceNotFoundException("Consumer pet not found"));
    TenantCustomer customer = customers.findByConsumerProfileIdAndTenantId(profile.id(), tenantId)
        .orElseGet(() -> {
          TenantCustomer linked = customers.save(TenantCustomer.linkConsumer(tenantId, profile.id(),
              profile.name(), profile.email(), profile.phone(), profile.document()));
          outbox.append("TenantCustomer", linked.id(), "customer.linked", tenantId,
              Map.of("customerId", linked.id(), "consumerProfileId", profile.id()));
          return linked;
        });
    TenantPet pet = tenantPets.findByConsumerPetIdAndTenantId(consumerPetId, tenantId)
        .filter(candidate -> candidate.tenantCustomerId().equals(customer.id()))
        .orElseGet(() -> {
          TenantPet linked = tenantPets.save(TenantPet.linkConsumer(tenantId, customer.id(),
              consumerPet.id(), consumerPet.name(), consumerPet.species(), consumerPet.breed(),
              consumerPet.birthDate(), consumerPet.sex(), consumerPet.weight()));
          outbox.append("TenantPet", linked.id(), "pet.linked", tenantId,
              Map.of("petId", linked.id(), "consumerPetId", consumerPet.id(),
                  "customerId", customer.id()));
          return linked;
        });
    return new LinkedCustomerPet(customer, pet);
  }

  public record LinkedCustomerPet(TenantCustomer customer, TenantPet pet) {}
}
