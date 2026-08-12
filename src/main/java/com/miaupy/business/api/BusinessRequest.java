package com.miaupy.business.api;

import com.miaupy.business.application.BusinessCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BusinessRequest(
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
    @Size(max = 32)
        @Schema(description = "Documento fiscal da empresa; não autentica a identidade por si só.")
        String document,
    @Size(max = 2000) @Schema(description = "Descrição pública da empresa.") String description,
    @Size(max = 32) @Schema(description = "Telefone de contato.", example = "+5511999999999")
        String phone,
    @Email
        @Size(max = 254)
        @Schema(description = "Endereço de e-mail válido.", example = "contato@example.com")
        String email,
    @Size(max = 500) @Schema(description = "URL pública da empresa.") String website,
    @Schema(description = "Indica se a empresa pode aparecer na vitrine pública.")
        boolean publicVisible) {
  BusinessCommand toCommand() {
    return new BusinessCommand(
        slug, name, tradeName, document, description, phone, email, website, publicVisible);
  }
}
