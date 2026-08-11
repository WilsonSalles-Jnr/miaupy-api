package com.miaupy.consumer.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.miaupy.consumer.domain.*;
import com.miaupy.shared.exception.ResourceNotFoundException;
import com.miaupy.shared.security.ActorContext;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsumerPetIsolationTest {
  @Test
  void consumerBDoesNotFindPetFromConsumerA() {
    UUID consumerB = UUID.randomUUID();
    UUID petFromA = UUID.randomUUID();
    ActorContext actors = mock(ActorContext.class);
    ConsumerProfileRepository profiles = mock(ConsumerProfileRepository.class);
    ConsumerPetRepository pets = mock(ConsumerPetRepository.class);
    ConsumerProfile profileB =
        new ConsumerProfile(
            consumerB,
            "consumer-b",
            "B",
            "b@example.com",
            null,
            null,
            null,
            true,
            Instant.now(),
            Instant.now(),
            0L);
    when(actors.getRequiredConsumerSubject()).thenReturn("consumer-b");
    when(profiles.findByAuthSubject("consumer-b")).thenReturn(Optional.of(profileB));
    when(pets.findByIdAndOwnerId(petFromA, consumerB)).thenReturn(Optional.empty());
    ConsumerPetUseCase useCase = new ConsumerPetUseCase(actors, profiles, pets);
    assertThatThrownBy(() -> useCase.get(petFromA)).isInstanceOf(ResourceNotFoundException.class);
    verify(pets).findByIdAndOwnerId(petFromA, consumerB);
  }
}
