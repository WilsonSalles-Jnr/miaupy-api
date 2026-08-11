package com.miaupy.onboarding.api;

import com.miaupy.onboarding.application.ProviderUpgradeCommand;
import com.miaupy.onboarding.application.UpgradeConsumerToProviderUseCase;
import com.miaupy.onboarding.domain.ProviderUpgrade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consumer/me/provider-upgrades")
public class ProviderUpgradeController {
  private final UpgradeConsumerToProviderUseCase useCase;

  public ProviderUpgradeController(UpgradeConsumerToProviderUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping
  public Response upgrade(
      @RequestHeader("Idempotency-Key") UUID idempotencyKey, @Valid @RequestBody Request request) {
    return Response.from(useCase.execute(idempotencyKey, request.toCommand()));
  }

  public record Request(
      @NotBlank
          @Size(max = 80)
          @Pattern(
              regexp = "[a-z0-9]+(?:-[a-z0-9]+)*",
              message = "must contain lowercase letters, numbers and single hyphens only")
          String slug,
      @NotBlank @Size(max = 160) String name,
      @Size(max = 160) String tradeName,
      @Size(max = 32) String document,
      @Size(max = 2000) String description,
      @Size(max = 32) String phone,
      @Email @Size(max = 254) String email,
      @Size(max = 500) String website) {
    ProviderUpgradeCommand toCommand() {
      return new ProviderUpgradeCommand(
          slug, name, tradeName, document, description, phone, email, website);
    }
  }

  public record Response(
      UUID upgradeId,
      Long tenantId,
      UUID businessId,
      ProviderUpgrade.Status status,
      String nextAction) {
    static Response from(ProviderUpgrade upgrade) {
      return new Response(
          upgrade.id(),
          upgrade.tenantId(),
          upgrade.businessId(),
          upgrade.status(),
          "Authenticate through the miaupy-business OIDC client to obtain a B2B token");
    }
  }
}
