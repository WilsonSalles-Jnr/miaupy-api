package com.miaupy.consumer.infrastructure.persistence;

import com.miaupy.consumer.domain.ConsumerPet;
import com.miaupy.consumer.domain.ConsumerPetRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
class ConsumerPetRepositoryAdapter implements ConsumerPetRepository {
    private final SpringDataConsumerPetRepository repository;
    ConsumerPetRepositoryAdapter(SpringDataConsumerPetRepository repository) { this.repository=repository; }
    public ConsumerPet save(ConsumerPet pet) { return map(repository.save(new ConsumerPetJpaEntity(pet))); }
    public Optional<ConsumerPet> findByIdAndOwnerId(UUID id, UUID ownerId) {
        return repository.findByIdAndConsumerProfileIdAndActiveTrue(id, ownerId).map(this::map);
    }
    public Page<ConsumerPet> findAllByOwnerId(UUID ownerId, Pageable pageable) {
        return repository.findAllByConsumerProfileIdAndActiveTrue(ownerId, pageable).map(this::map);
    }
    private ConsumerPet map(ConsumerPetJpaEntity e) {
        return new ConsumerPet(e.id,e.consumerProfileId,e.name,e.species,e.breed,e.birthDate,e.sex,e.weight,e.color,
                e.microchip,e.neutered,e.active,e.deletedAt,e.createdAt,e.updatedAt,e.version);
    }
}
