package com.miaupy.onboarding.application;

import com.miaupy.onboarding.domain.IdentityProvider;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class RegisterConsumerUseCase {
  private final IdentityProvider identityProvider;
  private final RegistrationRateLimiter rateLimiter;

  public RegisterConsumerUseCase(
      IdentityProvider identityProvider, RegistrationRateLimiter rateLimiter) {
    this.identityProvider = identityProvider;
    this.rateLimiter = rateLimiter;
  }

  public void execute(String remoteAddress, String name, String email, String password) {
    String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
    rateLimiter.check(remoteAddress, normalizedEmail);
    identityProvider.registerConsumer(name.strip(), normalizedEmail, password);
  }
}
