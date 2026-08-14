package com.miaupy.onboarding.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miaupy.onboarding.domain.BusinessRegistration;
import com.miaupy.onboarding.domain.IdentityProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RegisterBusinessUseCase {
  private final IdentityProvider identityProvider;
  private final RegistrationRateLimiter rateLimiter;
  private final BusinessRegistrationTransaction transaction;
  private final ObjectMapper objectMapper;

  public RegisterBusinessUseCase(
      IdentityProvider identityProvider,
      RegistrationRateLimiter rateLimiter,
      BusinessRegistrationTransaction transaction,
      ObjectMapper objectMapper) {
    this.identityProvider = identityProvider;
    this.rateLimiter = rateLimiter;
    this.transaction = transaction;
    this.objectMapper = objectMapper;
  }

  public void execute(
      String remoteAddress,
      UUID idempotencyKey,
      String ownerName,
      String email,
      String password,
      BusinessRegistrationCommand command) {
    String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
    String normalizedOwner = ownerName.strip();
    BusinessRegistrationCommand normalizedCommand =
        new BusinessRegistrationCommand(
            command.slug().strip().toLowerCase(Locale.ROOT),
            command.name().strip(),
            blankToNull(command.tradeName()),
            blankToNull(command.document()),
            blankToNull(command.description()),
            blankToNull(command.phone()),
            blankToNull(command.email()) == null
                ? normalizedEmail
                : command.email().strip().toLowerCase(Locale.ROOT),
            blankToNull(command.website()));
    rateLimiter.check(remoteAddress, normalizedEmail);
    String fingerprint = fingerprint(normalizedOwner, normalizedEmail, normalizedCommand);

    BusinessRegistration existing = transaction.find(idempotencyKey).orElse(null);
    if (existing != null) {
      transaction.ensureSameRequest(existing, fingerprint);
      finish(existing);
      return;
    }

    IdentityProvider.RegistrationResult identity =
        identityProvider.registerBusiness(normalizedOwner, normalizedEmail, password);
    if (identity.createdSubject().isEmpty()) return;

    String subject = identity.createdSubject().orElseThrow();
    BusinessRegistration registration;
    try {
      registration =
          transaction.prepare(subject, idempotencyKey, fingerprint, normalizedCommand);
    } catch (RuntimeException exception) {
      identityProvider.deleteUnverifiedUser(subject);
      throw exception;
    }
    finish(registration);
  }

  private void finish(BusinessRegistration registration) {
    if (registration.status() == BusinessRegistration.Status.COMPLETED) return;
    identityProvider.grantBusinessAccess(registration.authSubject(), registration.tenantId());
    identityProvider.sendBusinessVerification(registration.authSubject());
    transaction.complete(registration.id());
  }

  private String fingerprint(
      String ownerName, String normalizedEmail, BusinessRegistrationCommand command) {
    try {
      byte[] request =
          objectMapper.writeValueAsBytes(
              new FingerprintRequest(ownerName, normalizedEmail, command));
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(request));
    } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Unable to fingerprint business registration", exception);
    }
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private record FingerprintRequest(
      String ownerName, String email, BusinessRegistrationCommand business) {}
}
