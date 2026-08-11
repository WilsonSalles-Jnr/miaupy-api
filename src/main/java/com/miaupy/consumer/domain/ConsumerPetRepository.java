package com.miaupy.consumer.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConsumerPetRepository {
    ConsumerPet save(ConsumerPet pet);
    Optional<ConsumerPet> findByIdAndOwnerId(UUID id, UUID ownerId);
    Page<ConsumerPet> findAllByOwnerId(UUID ownerId, Pageable pageable);
}
