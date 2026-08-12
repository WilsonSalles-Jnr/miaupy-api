package com.miaupy.consumer.api;

import com.miaupy.consumer.application.ConsumerProfileUseCase;
import com.miaupy.consumer.domain.ConsumerProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consumer/me")
public class ConsumerProfileController {
  private final ConsumerProfileUseCase useCase;

  public ConsumerProfileController(ConsumerProfileUseCase useCase) {
    this.useCase = useCase;
  }

  @GetMapping
  @Operation(
      summary = "Consultar meu perfil",
      description =
          "Obtém o perfil B2C identificado pelo claim sub do JWT. No primeiro acesso com e-mail verificado, cria o perfil automaticamente a partir dos claims name e email.")
  public Response get() {
    return Response.from(useCase.getMe());
  }

  @PutMapping
  @Operation(
      summary = "Criar ou atualizar meu perfil",
      description =
          "Cria ou atualiza o perfil do consumidor autenticado sem aceitar consumerId externo.")
  public Response upsert(@Valid @RequestBody Request request) {
    return Response.from(
        useCase.upsert(
            request.name(),
            request.email(),
            request.phone(),
            request.document(),
            request.birthDate()));
  }

  public record Request(
      @NotBlank @Size(max = 160) @Schema(description = "Nome completo do consumidor.") String name,
      @NotBlank
          @Email
          @Size(max = 254)
          @Schema(description = "Endereço de e-mail válido.", example = "contato@example.com")
          String email,
      @Size(max = 32) @Schema(description = "Telefone de contato.", example = "+5511999999999")
          String phone,
      @Size(max = 32) @Schema(description = "Documento pessoal do consumidor.") String document,
      @Past
          @Schema(description = "Data de nascimento no formato YYYY-MM-DD.", example = "1990-05-10")
          LocalDate birthDate) {}

  public record Response(
      UUID id, String name, String email, String phone, String document, LocalDate birthDate) {
    static Response from(ConsumerProfile p) {
      return new Response(p.id(), p.name(), p.email(), p.phone(), p.document(), p.birthDate());
    }
  }
}
