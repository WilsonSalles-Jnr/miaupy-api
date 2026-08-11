package com.miaupy.consumer.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataConsumerPetRepository extends JpaRepository<ConsumerPetJpaEntity, UUID> {
  Optional<ConsumerPetJpaEntity> findByIdAndConsumerProfileIdAndActiveTrue(UUID id, UUID ownerId);

  Page<ConsumerPetJpaEntity> findAllByConsumerProfileIdAndActiveTrue(
      UUID ownerId, Pageable pageable);
}
