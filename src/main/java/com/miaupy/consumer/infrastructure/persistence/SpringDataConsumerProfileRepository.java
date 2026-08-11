package com.miaupy.consumer.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataConsumerProfileRepository extends JpaRepository<ConsumerProfileJpaEntity, UUID> {
    Optional<ConsumerProfileJpaEntity> findByAuthSubjectAndActiveTrue(String authSubject);
}
