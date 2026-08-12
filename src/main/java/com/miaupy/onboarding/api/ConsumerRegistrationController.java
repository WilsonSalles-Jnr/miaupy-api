package com.miaupy.onboarding.api;

import com.miaupy.onboarding.application.RegisterConsumerUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/consumers")
public class ConsumerRegistrationController {
  private static final String GENERIC_MESSAGE =
      "If the registration can be accepted, verification instructions will be sent by email.";

  private final RegisterConsumerUseCase useCase;

  public ConsumerRegistrationController(RegisterConsumerUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping("/registrations")
  @Operation(
      summary = "Solicitar cadastro de consumidor",
      description =
          "Cria a identidade B2C no provedor externo, exige verificação de e-mail e sempre retorna mensagem genérica para impedir enumeração de contas. A origem e o e-mail protegido por HMAC possuem limites independentes.")
  public ResponseEntity<Response> register(
      @Valid @RequestBody Request request, HttpServletRequest httpRequest) {
    useCase.execute(
        httpRequest.getRemoteAddr(), request.name(), request.email(), request.password());
    return ResponseEntity.accepted().body(new Response(GENERIC_MESSAGE));
  }

  public record Request(
      @NotBlank @Size(max = 160) @Schema(description = "Nome completo do consumidor.") String name,
      @NotBlank
          @Email
          @Size(max = 254)
          @Schema(description = "Endereço de e-mail válido.", example = "contato@example.com")
          String email,
      @NotBlank
          @Size(min = 12, max = 128)
          @Schema(
              description =
                  "Senha enviada somente ao provedor de identidade, com 12 a 128 caracteres; nunca é persistida ou registrada em log pela API.",
              example = "example-only-passphrase")
          String password,
      @AssertTrue(message = "must be accepted")
          @Schema(description = "Confirmação obrigatória de aceite dos termos da plataforma.")
          boolean termsAccepted) {}

  public record Response(String message) {}
}
