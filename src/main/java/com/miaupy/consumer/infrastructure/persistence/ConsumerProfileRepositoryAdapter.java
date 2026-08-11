package com.miaupy.consumer.infrastructure.persistence;

import com.miaupy.consumer.domain.ConsumerProfile;
import com.miaupy.consumer.domain.ConsumerProfileRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class ConsumerProfileRepositoryAdapter implements ConsumerProfileRepository {
  private final SpringDataConsumerProfileRepository repository;

  ConsumerProfileRepositoryAdapter(SpringDataConsumerProfileRepository repository) {
    this.repository = repository;
  }

  public ConsumerProfile save(ConsumerProfile p) {
    return map(repository.save(map(p)));
  }

  public Optional<ConsumerProfile> findByAuthSubject(String subject) {
    return repository.findByAuthSubjectAndActiveTrue(subject).map(this::map);
  }

  public Optional<ConsumerProfile> findById(UUID id) {
    return repository.findById(id).filter(e -> e.active).map(this::map);
  }

  private ConsumerProfileJpaEntity map(ConsumerProfile p) {
    return new ConsumerProfileJpaEntity(
        p.id(),
        p.authSubject(),
        p.name(),
        p.email(),
        p.phone(),
        p.document(),
        p.birthDate(),
        p.active(),
        p.createdAt(),
        p.updatedAt(),
        p.version());
  }

  private ConsumerProfile map(ConsumerProfileJpaEntity e) {
    return new ConsumerProfile(
        e.id,
        e.authSubject,
        e.name,
        e.email,
        e.phone,
        e.document,
        e.birthDate,
        e.active,
        e.createdAt,
        e.updatedAt,
        e.version);
  }
}
