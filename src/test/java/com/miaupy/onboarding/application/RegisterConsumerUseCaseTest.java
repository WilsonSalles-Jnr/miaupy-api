package com.miaupy.onboarding.application;

import static org.mockito.Mockito.verify;

import com.miaupy.onboarding.domain.IdentityProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterConsumerUseCaseTest {
  @Mock IdentityProvider identityProvider;
  @Mock RegistrationRateLimiter rateLimiter;

  @Test
  void normalizesEmailBeforeRateLimitingAndIdentityCreation() {
    RegisterConsumerUseCase useCase = new RegisterConsumerUseCase(identityProvider, rateLimiter);

    useCase.execute("127.0.0.1", " Jane Doe ", " Jane@Example.COM ", "safe passphrase 123");

    verify(rateLimiter).check("127.0.0.1", "jane@example.com");
    verify(identityProvider)
        .registerConsumer("Jane Doe", "jane@example.com", "safe passphrase 123");
  }
}
