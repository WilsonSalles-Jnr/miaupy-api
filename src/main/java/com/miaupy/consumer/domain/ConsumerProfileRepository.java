package com.miaupy.consumer.domain;

import java.util.Optional;
import java.util.UUID;

public interface ConsumerProfileRepository {
  ConsumerProfile save(ConsumerProfile profile);

  Optional<ConsumerProfile> findByAuthSubject(String authSubject);

  Optional<ConsumerProfile> findById(UUID id);
}
