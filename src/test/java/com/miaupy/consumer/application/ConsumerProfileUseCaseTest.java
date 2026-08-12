package com.miaupy.consumer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miaupy.consumer.domain.ConsumerProfile;
import com.miaupy.consumer.domain.ConsumerProfileRepository;
import com.miaupy.shared.security.ActorContext;
import com.miaupy.shared.security.ConsumerIdentity;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsumerProfileUseCaseTest {
  @Mock ActorContext actorContext;
  @Mock ConsumerProfileRepository repository;
  @Mock ConsumerProfileProvisioningLock provisioningLock;

  @Test
  void provisionsProfileFromVerifiedIdentityOnFirstGet() {
    ConsumerIdentity identity =
        new ConsumerIdentity("consumer-123", "Jane Doe", "jane@example.com");
    when(actorContext.getRequiredVerifiedConsumerIdentity()).thenReturn(identity);
    when(repository.findByAuthSubject(identity.subject())).thenReturn(Optional.empty());
    when(repository.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    ConsumerProfileUseCase useCase =
        new ConsumerProfileUseCase(actorContext, repository, provisioningLock);

    ConsumerProfile profile = useCase.getMe();

    assertThat(profile.authSubject()).isEqualTo(identity.subject());
    assertThat(profile.name()).isEqualTo(identity.name());
    assertThat(profile.email()).isEqualTo(identity.email());
    assertThat(profile.phone()).isNull();
    verify(provisioningLock).lock(identity.subject());
  }

  @Test
  void returnsExistingProfileWithoutLocking() {
    ConsumerIdentity identity =
        new ConsumerIdentity("consumer-123", "Jane Doe", "jane@example.com");
    ConsumerProfile existing =
        ConsumerProfile.create(
            identity.subject(), identity.name(), identity.email(), null, null, null);
    when(actorContext.getRequiredVerifiedConsumerIdentity()).thenReturn(identity);
    when(repository.findByAuthSubject(identity.subject())).thenReturn(Optional.of(existing));
    ConsumerProfileUseCase useCase =
        new ConsumerProfileUseCase(actorContext, repository, provisioningLock);

    assertThat(useCase.getMe()).isSameAs(existing);

    verify(provisioningLock, never()).lock(identity.subject());
    verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
  }
}
