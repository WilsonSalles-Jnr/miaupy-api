package com.miaupy.shared.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiDocumentationCustomizer {
  @Bean
  OpenApiCustomizer globalEndpointDocumentation() {
    return openApi ->
        openApi
            .getPaths()
            .forEach(
                (path, item) ->
                    item.readOperationsMap()
                        .forEach(
                            (method, operation) -> {
                              operation.setTags(List.of(tag(path)));
                              addCorrelationHeader(operation);
                              documentResponses(operation, method, path);
                              if (isPublic(path)) {
                                operation.setSecurity(List.of());
                              } else {
                                operation.addSecurityItem(
                                    new SecurityRequirement().addList(OpenApiConfig.BEARER_AUTH));
                              }
                            }));
  }

  private void addCorrelationHeader(Operation operation) {
    boolean alreadyDocumented =
        operation.getParameters() != null
            && operation.getParameters().stream()
                .anyMatch(parameter -> "X-Correlation-ID".equals(parameter.getName()));
    if (!alreadyDocumented) {
      operation.addParametersItem(
          new Parameter()
              .name("X-Correlation-ID")
              .in("header")
              .required(false)
              .description(
                  "Identificador opcional de correlação, com até 64 caracteres alfanuméricos, ponto, hífen ou underscore.")
              .schema(new StringSchema().example("request-7f3f0c1a")));
    }
  }

  private void documentResponses(Operation operation, HttpMethod method, String path) {
    operation
        .getResponses()
        .forEach((code, response) -> response.setDescription(success(code, method)));
    operation
        .getResponses()
        .addApiResponse("400", response("Request inválido ou parâmetros malformados."));
    if (!isPublic(path)) {
      operation
          .getResponses()
          .addApiResponse("401", response("JWT ausente, inválido ou expirado."));
      operation
          .getResponses()
          .addApiResponse("403", response("Ator sem permissão ou contexto incompatível."));
    }
    if (path.contains("{") || method == HttpMethod.GET) {
      operation
          .getResponses()
          .addApiResponse("404", response("Recurso não encontrado no contexto permitido."));
    }
    if (method != HttpMethod.GET) {
      operation
          .getResponses()
          .addApiResponse("409", response("Conflito de estado, unicidade ou concorrência."));
      operation.getResponses().addApiResponse("422", response("Regra de domínio não satisfeita."));
    }
  }

  private ApiResponse response(String description) {
    return new ApiResponse().description(description);
  }

  private String success(String code, HttpMethod method) {
    return switch (code) {
      case "201" -> "Recurso criado com sucesso.";
      case "204" -> "Operação concluída sem corpo de resposta.";
      default ->
          method == HttpMethod.GET
              ? "Consulta realizada com sucesso."
              : "Operação realizada com sucesso.";
    };
  }

  private String tag(String path) {
    if (path.startsWith("/api/v1/auth/")) return "Cadastro e identidade";
    if (path.contains("provider-upgrades")) return "Upgrade para fornecedor";
    if (path.startsWith("/api/v1/public/"))
      return path.endsWith("/availability") ? "Disponibilidade" : "Vitrine pública";
    if (path.startsWith("/api/v1/consumer/me/appointments")) return "Agendamentos B2C";
    if (path.startsWith("/api/v1/consumer/me/cart")) return "Carrinho B2C";
    if (path.startsWith("/api/v1/consumer/me/orders")) return "Pedidos B2C";
    if (path.startsWith("/api/v1/consumer/me/pets")) return "Pets B2C";
    if (path.equals("/api/v1/consumer/me")) return "Perfil B2C";
    if (path.contains("availability-rules")) return "Disponibilidade";
    if (path.contains("appointments")) return "Agendamentos B2B";
    if (path.contains("orders")) return "Pedidos B2B";
    if (path.contains("products")) return "Produtos";
    if (path.contains("services")) return "Serviços";
    if (path.contains("customers")) return path.endsWith("/pets") ? "Pets do tenant" : "Clientes";
    if (path.contains("/business/pets")) return "Pets do tenant";
    return "Empresa";
  }

  private static boolean isPublic(String path) {
    return path.startsWith("/api/v1/public/")
        || path.equals("/api/v1/auth/consumers/registrations");
  }
}
