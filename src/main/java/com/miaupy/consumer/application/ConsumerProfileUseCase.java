package com.miaupy.consumer.application;

import com.miaupy.consumer.domain.ConsumerProfile;
import com.miaupy.consumer.domain.ConsumerProfileRepository;
import com.miaupy.shared.security.ActorContext;
import com.miaupy.shared.security.ConsumerIdentity;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsumerProfileUseCase {
  private final ActorContext actorContext;
  private final ConsumerProfileRepository repository;
  private final ConsumerProfileProvisioningLock provisioningLock;

  public ConsumerProfileUseCase(
      ActorContext actorContext,
      ConsumerProfileRepository repository,
      ConsumerProfileProvisioningLock provisioningLock) {
    this.actorContext = actorContext;
    this.repository = repository;
    this.provisioningLock = provisioningLock;
  }

  @Transactional
  public ConsumerProfile getMe() {
    ConsumerIdentity identity = actorContext.getRequiredVerifiedConsumerIdentity();
    return repository.findByAuthSubject(identity.subject()).orElseGet(() -> provision(identity));
  }

  @Transactional
  public ConsumerProfile upsert(
      String name, String email, String phone, String document, LocalDate birthDate) {
    String subject = actorContext.getRequiredConsumerSubject();
    ConsumerProfile profile =
        repository
            .findByAuthSubject(subject)
            .map(current -> current.update(name, email, phone, document, birthDate))
            .orElseGet(
                () -> ConsumerProfile.create(subject, name, email, phone, document, birthDate));
    return repository.save(profile);
  }

  private ConsumerProfile provision(ConsumerIdentity identity) {
    provisioningLock.lock(identity.subject());
    return repository
        .findByAuthSubject(identity.subject())
        .orElseGet(
            () ->
                repository.save(
                    ConsumerProfile.create(
                        identity.subject(), identity.name(), identity.email(), null, null, null)));
  }
}
