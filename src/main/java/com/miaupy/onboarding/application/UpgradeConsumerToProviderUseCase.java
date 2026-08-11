package com.miaupy.onboarding.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miaupy.onboarding.domain.IdentityProvider;
import com.miaupy.onboarding.domain.ProviderUpgrade;
import com.miaupy.shared.exception.ActorAccessDeniedException;
import com.miaupy.shared.security.ActorContext;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UpgradeConsumerToProviderUseCase {
  private final ActorContext actorContext;
  private final ProviderUpgradeTransaction transaction;
  private final IdentityProvider identityProvider;
  private final ObjectMapper objectMapper;

  public UpgradeConsumerToProviderUseCase(
      ActorContext actorContext,
      ProviderUpgradeTransaction transaction,
      IdentityProvider identityProvider,
      ObjectMapper objectMapper) {
    this.actorContext = actorContext;
    this.transaction = transaction;
    this.identityProvider = identityProvider;
    this.objectMapper = objectMapper;
  }

  public ProviderUpgrade execute(UUID idempotencyKey, ProviderUpgradeCommand command) {
    String subject = actorContext.getRequiredConsumerSubject();
    if (!actorContext.isEmailVerified()) {
      throw new ActorAccessDeniedException("A verified email is required for a provider upgrade");
    }
    ProviderUpgrade upgrade =
        transaction.prepare(subject, idempotencyKey, fingerprint(command), command);
    if (upgrade.status() == ProviderUpgrade.Status.COMPLETED) {
      return upgrade;
    }
    identityProvider.grantBusinessAccess(subject, upgrade.tenantId());
    return transaction.complete(upgrade.id());
  }

  private String fingerprint(ProviderUpgradeCommand command) {
    try {
      byte[] request = objectMapper.writeValueAsBytes(command);
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(request));
    } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Unable to fingerprint provider upgrade", exception);
    }
  }
}
