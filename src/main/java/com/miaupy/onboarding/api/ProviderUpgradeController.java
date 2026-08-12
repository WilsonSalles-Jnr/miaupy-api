package com.miaupy.onboarding.api;

import com.miaupy.onboarding.application.ProviderUpgradeCommand;
import com.miaupy.onboarding.application.UpgradeConsumerToProviderUseCase;
import com.miaupy.onboarding.domain.ProviderUpgrade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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
  @Operation(
      summary = "Fazer upgrade para empresa fornecedora",
      description =
          "Cria tenant, empresa e configurações de forma atômica e concede OWNER no provedor de identidade. Exige ator B2C com e-mail verificado e Idempotency-Key UUID; retries seguros retomam o workflow sem duplicar empresa.")
  public Response upgrade(
      @Parameter(
              description =
                  "UUID único da solicitação. Reutilize o mesmo valor em retries do mesmo upgrade; um corpo diferente gera 409.")
          @RequestHeader("Idempotency-Key")
          UUID idempotencyKey,
      @Valid @RequestBody Request request) {
    return Response.from(useCase.execute(idempotencyKey, request.toCommand()));
  }

  public record Request(
      @NotBlank
          @Size(max = 80)
          @Pattern(
              regexp = "[a-z0-9]+(?:-[a-z0-9]+)*",
              message = "must contain lowercase letters, numbers and single hyphens only")
          @Schema(description = "Slug público único da empresa.", example = "clinica-pet-centro")
          String slug,
      @NotBlank @Size(max = 160) @Schema(description = "Razão social ou nome da empresa.")
          String name,
      @Size(max = 160) @Schema(description = "Nome fantasia da empresa.") String tradeName,
      @Size(max = 32) @Schema(description = "Documento fiscal da empresa.") String document,
      @Size(max = 2000) @Schema(description = "Descrição pública da empresa.") String description,
      @Size(max = 32) @Schema(description = "Telefone de contato.") String phone,
      @Email @Size(max = 254) @Schema(description = "Endereço de e-mail válido.") String email,
      @Size(max = 500) @Schema(description = "URL pública da empresa.") String website) {
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
