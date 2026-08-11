package com.miaupy.onboarding.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miaupy.onboarding.domain.IdentityProvider;
import com.miaupy.onboarding.domain.ProviderUpgrade;
import com.miaupy.shared.exception.ActorAccessDeniedException;
import com.miaupy.shared.security.ActorContext;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpgradeConsumerToProviderUseCaseTest {
  @Mock ActorContext actorContext;
  @Mock ProviderUpgradeTransaction transaction;
  @Mock IdentityProvider identityProvider;

  private UpgradeConsumerToProviderUseCase useCase;
  private final ProviderUpgradeCommand command =
      new ProviderUpgradeCommand(
          "pet-store", "Pet Store", null, null, null, null, "owner@example.com", null);

  @BeforeEach
  void setUp() {
    useCase =
        new UpgradeConsumerToProviderUseCase(
            actorContext, transaction, identityProvider, new ObjectMapper());
  }

  @Test
  void rejectsUpgradeWhenEmailIsNotVerified() {
    when(actorContext.getRequiredConsumerSubject()).thenReturn("consumer-subject");
    when(actorContext.isEmailVerified()).thenReturn(false);

    assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), command))
        .isInstanceOf(ActorAccessDeniedException.class);

    verify(transaction, never())
        .prepare(
            anyString(),
            org.mockito.ArgumentMatchers.any(),
            anyString(),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void completedRetryDoesNotGrantIdentityTwice() {
    UUID key = UUID.randomUUID();
    ProviderUpgrade completed =
        new ProviderUpgrade(
            UUID.randomUUID(),
            "consumer-subject",
            UUID.randomUUID(),
            key,
            "fingerprint",
            50000001L,
            UUID.randomUUID(),
            ProviderUpgrade.Status.COMPLETED,
            Instant.now(),
            Instant.now());
    when(actorContext.getRequiredConsumerSubject()).thenReturn("consumer-subject");
    when(actorContext.isEmailVerified()).thenReturn(true);
    when(transaction.prepare(
            org.mockito.ArgumentMatchers.eq("consumer-subject"),
            org.mockito.ArgumentMatchers.eq(key),
            anyString(),
            org.mockito.ArgumentMatchers.eq(command)))
        .thenReturn(completed);

    useCase.execute(key, command);

    verify(identityProvider, never())
        .grantBusinessAccess(anyString(), org.mockito.ArgumentMatchers.anyLong());
    verify(transaction, never()).complete(org.mockito.ArgumentMatchers.any());
  }
}
