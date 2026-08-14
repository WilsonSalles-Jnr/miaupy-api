package com.miaupy.onboarding.application;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miaupy.onboarding.domain.BusinessRegistration;
import com.miaupy.onboarding.domain.IdentityProvider;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterBusinessUseCaseTest {
  @Mock IdentityProvider identities;
  @Mock RegistrationRateLimiter rateLimiter;
  @Mock BusinessRegistrationTransaction transaction;

  private RegisterBusinessUseCase useCase;
  private final BusinessRegistrationCommand command =
      new BusinessRegistrationCommand(
          "pet-store", "Pet Store", null, null, null, null, null, null);

  @BeforeEach
  void setUp() {
    useCase =
        new RegisterBusinessUseCase(identities, rateLimiter, transaction, new ObjectMapper());
  }

  @Test
  void createsBusinessIdentityWithoutConsumerProfile() {
    UUID key = UUID.randomUUID();
    UUID registrationId = UUID.randomUUID();
    when(transaction.find(key)).thenReturn(Optional.empty());
    when(identities.registerBusiness("Owner", "owner@example.com", "safe passphrase 123"))
        .thenReturn(new IdentityProvider.RegistrationResult(true, "business-subject"));
    when(transaction.prepare(
            org.mockito.ArgumentMatchers.eq("business-subject"),
            org.mockito.ArgumentMatchers.eq(key),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(
            new BusinessRegistration(
                registrationId,
                "business-subject",
                key,
                "fingerprint",
                50000001L,
                UUID.randomUUID(),
                BusinessRegistration.Status.LOCAL_READY,
                Instant.now(),
                Instant.now()));

    useCase.execute(
        "127.0.0.1",
        key,
        " Owner ",
        " Owner@Example.com ",
        "safe passphrase 123",
        command);

    verify(rateLimiter).check("127.0.0.1", "owner@example.com");
    verify(identities).grantBusinessAccess("business-subject", 50000001L);
    verify(identities).sendBusinessVerification("business-subject");
    verify(transaction).complete(registrationId);
  }

  @Test
  void existingEmailKeepsGenericFlowWithoutCreatingTenant() {
    UUID key = UUID.randomUUID();
    when(transaction.find(key)).thenReturn(Optional.empty());
    when(identities.registerBusiness("Owner", "owner@example.com", "safe passphrase 123"))
        .thenReturn(new IdentityProvider.RegistrationResult(false, null));

    useCase.execute(
        "127.0.0.1",
        key,
        "Owner",
        "owner@example.com",
        "safe passphrase 123",
        command);

    verify(transaction, never())
        .prepare(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any());
  }
}
