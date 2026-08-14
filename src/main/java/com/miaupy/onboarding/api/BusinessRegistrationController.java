package com.miaupy.onboarding.api;

import com.miaupy.onboarding.application.BusinessRegistrationCommand;
import com.miaupy.onboarding.application.RegisterBusinessUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/businesses")
public class BusinessRegistrationController {
  private static final String GENERIC_MESSAGE =
      "If the registration can be accepted, business verification instructions will be sent by email.";

  private final RegisterBusinessUseCase useCase;

  public BusinessRegistrationController(RegisterBusinessUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping("/registrations")
  @Operation(
      summary = "Cadastrar empresa e proprietário",
      description =
          "Cria a identidade B2B, tenant, empresa e configurações. O tenant é alocado exclusivamente pelo servidor; a identidade recebe OWNER e deve verificar o e-mail antes do login empresarial.")
  public ResponseEntity<Response> register(
      @Parameter(description = "UUID reutilizado somente em tentativas do mesmo cadastro.")
          @RequestHeader("Idempotency-Key")
          UUID idempotencyKey,
      @Valid @RequestBody Request request,
      HttpServletRequest httpRequest) {
    useCase.execute(
        httpRequest.getRemoteAddr(),
        idempotencyKey,
        request.ownerName(),
        request.email(),
        request.password(),
        request.toCommand());
    return ResponseEntity.accepted().body(new Response(GENERIC_MESSAGE));
  }

  public record Request(
      @NotBlank @Size(max = 160) @Schema(description = "Nome do proprietário da conta empresarial.")
          String ownerName,
      @NotBlank @Email @Size(max = 254) @Schema(description = "E-mail usado para login e verificação.")
          String email,
      @NotBlank @Size(min = 12, max = 128) @Schema(description = "Senha enviada somente ao provedor de identidade.")
          String password,
      @AssertTrue(message = "must be accepted") @Schema(description = "Aceite obrigatório dos termos da plataforma.")
          boolean termsAccepted,
      @NotBlank
          @Size(max = 80)
          @Pattern(
              regexp = "[a-z0-9]+(?:-[a-z0-9]+)*",
              message = "must contain lowercase letters, numbers and single hyphens only")
          @Schema(description = "Slug público único da empresa.") String slug,
      @NotBlank @Size(max = 160) @Schema(description = "Razão social ou nome empresarial.")
          String name,
      @Size(max = 160) @Schema(description = "Nome fantasia da empresa.") String tradeName,
      @Size(max = 32) @Schema(description = "Documento fiscal da empresa.") String document,
      @Size(max = 2000) @Schema(description = "Descrição exibida na vitrine pública.")
          String description,
      @Size(max = 32) @Schema(description = "Telefone comercial.") String phone,
      @Email @Size(max = 254) @Schema(description = "E-mail público; usa o e-mail do proprietário quando omitido.")
          String businessEmail,
      @Size(max = 500) @Schema(description = "Website público da empresa.") String website) {
    BusinessRegistrationCommand toCommand() {
      return new BusinessRegistrationCommand(
          slug, name, tradeName, document, description, phone, businessEmail, website);
    }
  }

  public record Response(String message) {}
}
