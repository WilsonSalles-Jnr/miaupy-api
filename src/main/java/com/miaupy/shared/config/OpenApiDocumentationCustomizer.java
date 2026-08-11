package com.miaupy.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiDocumentationCustomizer {
  private static final Map<String, EndpointDoc> ENDPOINTS = endpointDocs();
  private static final Map<String, String> PARAMETERS = parameterDescriptions();
  private static final Map<String, String> PROPERTIES = propertyDescriptions();

  @Bean
  OpenApiCustomizer detailedEndpointDocumentation() {
    return openApi -> {
      documentOperations(openApi);
      documentSchemas(openApi);
    };
  }

  private void documentOperations(OpenAPI openApi) {
    openApi
        .getPaths()
        .forEach(
            (path, item) ->
                item.readOperationsMap()
                    .forEach(
                        (method, operation) -> {
                          EndpointDoc doc = ENDPOINTS.get(method.name() + " " + path);
                          if (doc == null) {
                            throw new IllegalStateException(
                                "Endpoint without OpenAPI documentation: " + method + " " + path);
                          }
                          operation.summary(doc.summary()).description(doc.description());
                          operation.setTags(java.util.List.of(tag(path)));
                          documentParameters(operation);
                          documentResponses(operation, method, path);
                          if (isPublic(path)) {
                            operation.setSecurity(java.util.List.of());
                          } else {
                            operation.addSecurityItem(
                                new SecurityRequirement().addList(OpenApiConfig.BEARER_AUTH));
                          }
                        }));
  }

  private void documentParameters(Operation operation) {
    if (operation.getParameters() != null) {
      operation
          .getParameters()
          .forEach(
              parameter -> {
                parameter.setDescription(
                    PARAMETERS.getOrDefault(
                        parameter.getName(), "Parâmetro necessário para executar a operação."));
                if (parameter.getSchema() != null && parameter.getSchema().getExample() == null) {
                  parameter.getSchema().setExample(parameterExample(parameter.getName()));
                }
              });
    }
    if (operation.getRequestBody() != null) {
      operation
          .getRequestBody()
          .setDescription(
              "Corpo JSON da operação. As restrições de obrigatoriedade, tamanho, formato e domínio são detalhadas em cada propriedade do schema.");
    }
    operation.addParametersItem(
        new Parameter()
            .name("X-Correlation-ID")
            .in("header")
            .required(false)
            .description(
                "Identificador opcional de correlação, com até 64 caracteres alfanuméricos, ponto, hífen ou underscore.")
            .schema(new StringSchema().example("request-7f3f0c1a")));
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

  private void documentSchemas(OpenAPI openApi) {
    if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
      return;
    }
    openApi
        .getComponents()
        .getSchemas()
        .values()
        .forEach(
            schema -> {
              if (schema.getProperties() == null) {
                return;
              }
              schema
                  .getProperties()
                  .forEach(
                      (name, property) -> {
                        String propertyName = String.valueOf(name);
                        Schema<?> value = (Schema<?>) property;
                        value.setDescription(
                            PROPERTIES.getOrDefault(
                                propertyName, "Campo do recurso conforme o contrato da operação."));
                        String example = propertyExample(propertyName);
                        if (example != null && value.getExample() == null) {
                          value.setExample(example);
                        }
                      });
            });
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
    if (path.startsWith("/api/v1/consumer/me/pets")) return "Pets B2C";
    if (path.equals("/api/v1/consumer/me")) return "Perfil B2C";
    if (path.contains("availability-rules")) return "Disponibilidade";
    if (path.contains("appointments")) return "Agendamentos B2B";
    if (path.contains("products")) return "Produtos";
    if (path.contains("services")) return "Serviços";
    if (path.contains("customers")) return path.endsWith("/pets") ? "Pets do tenant" : "Clientes";
    if (path.contains("/business/pets")) return "Pets do tenant";
    return "Empresa";
  }

  private static Map<String, EndpointDoc> endpointDocs() {
    Map<String, EndpointDoc> d = new LinkedHashMap<>();
    add(
        d,
        "POST",
        "/api/v1/auth/consumers/registrations",
        "Solicitar cadastro de consumidor",
        "Cria a identidade B2C no provedor externo, exige verificação de e-mail e sempre retorna mensagem genérica para impedir enumeração de contas. A origem e o e-mail protegido por HMAC possuem limites independentes.");
    add(
        d,
        "POST",
        "/api/v1/consumer/me/provider-upgrades",
        "Fazer upgrade para empresa fornecedora",
        "Cria tenant, empresa e configurações de forma atômica e concede OWNER no provedor de identidade. Exige ator B2C com e-mail verificado e Idempotency-Key UUID; retries seguros retomam o workflow sem duplicar empresa.");
    add(
        d,
        "POST",
        "/api/v1/business/profile",
        "Criar perfil empresarial",
        "Cria o perfil do tenant autenticado. O tenant_id é obtido exclusivamente do JWT.");
    add(
        d,
        "GET",
        "/api/v1/business/profile",
        "Consultar perfil empresarial",
        "Retorna o perfil da empresa pertencente ao tenant autenticado.");
    add(
        d,
        "PUT",
        "/api/v1/business/profile",
        "Atualizar perfil empresarial",
        "Atualiza dados comerciais e a visibilidade pública da empresa autenticada.");
    add(
        d,
        "GET",
        "/api/v1/business/settings",
        "Consultar configurações",
        "Retorna timezone, moeda e permissões de operação online do tenant.");
    add(
        d,
        "PUT",
        "/api/v1/business/settings",
        "Atualizar configurações",
        "Define aprovação de agenda, timezone, moeda, vendas e agendamentos online.");
    add(
        d,
        "GET",
        "/api/v1/business/address",
        "Consultar endereço empresarial",
        "Retorna o endereço da empresa do tenant autenticado.");
    add(
        d,
        "PUT",
        "/api/v1/business/address",
        "Atualizar endereço empresarial",
        "Cria ou atualiza endereço e coordenadas geográficas da empresa.");
    add(
        d,
        "GET",
        "/api/v1/public/stores/{slug}",
        "Consultar loja pública",
        "Retorna somente empresa ativa e marcada como publicamente visível.");
    add(
        d,
        "GET",
        "/api/v1/public/stores/{slug}/products",
        "Listar produtos públicos",
        "Lista paginada de produtos ativos e publicados, com cache Redis e fallback PostgreSQL.");
    add(
        d,
        "GET",
        "/api/v1/public/stores/{slug}/products/{productId}",
        "Consultar produto público",
        "Retorna um produto ativo e publicado pertencente à loja informada.");
    add(
        d,
        "GET",
        "/api/v1/public/stores/{slug}/services",
        "Listar serviços públicos",
        "Lista paginada de serviços ativos e publicados da loja.");
    add(
        d,
        "GET",
        "/api/v1/consumer/me",
        "Consultar meu perfil",
        "Obtém o perfil B2C identificado pelo claim sub do JWT.");
    add(
        d,
        "PUT",
        "/api/v1/consumer/me",
        "Criar ou atualizar meu perfil",
        "Cria ou atualiza o perfil do consumidor autenticado sem aceitar consumerId externo.");
    add(
        d,
        "POST",
        "/api/v1/consumer/me/pets",
        "Cadastrar meu pet",
        "Cadastra um pet global pertencente ao consumidor autenticado.");
    add(
        d,
        "GET",
        "/api/v1/consumer/me/pets",
        "Listar meus pets",
        "Lista paginada de pets ativos pertencentes ao claim sub autenticado.");
    add(
        d,
        "GET",
        "/api/v1/consumer/me/pets/{id}",
        "Consultar meu pet",
        "Retorna o pet somente quando pertence ao consumidor autenticado.");
    add(
        d,
        "PUT",
        "/api/v1/consumer/me/pets/{id}",
        "Atualizar meu pet",
        "Atualiza dados do pet pertencente ao consumidor autenticado.");
    add(
        d,
        "DELETE",
        "/api/v1/consumer/me/pets/{id}",
        "Desativar meu pet",
        "Executa exclusão lógica do pet do consumidor autenticado.");
    add(
        d,
        "GET",
        "/api/v1/business/customers",
        "Listar clientes",
        "Lista paginada do CRM filtrada pelo tenant do JWT.");
    add(
        d,
        "POST",
        "/api/v1/business/customers",
        "Cadastrar cliente",
        "Cadastra cliente no CRM do tenant sem realizar vínculo B2C automático.");
    add(
        d,
        "GET",
        "/api/v1/business/customers/{id}",
        "Consultar cliente",
        "Consulta cliente utilizando obrigatoriamente id e tenant_id.");
    add(
        d,
        "PUT",
        "/api/v1/business/customers/{id}",
        "Atualizar cliente",
        "Atualiza os dados do cliente dentro do tenant autenticado.");
    add(
        d,
        "DELETE",
        "/api/v1/business/customers/{id}",
        "Desativar cliente",
        "Realiza exclusão lógica do cliente do CRM.");
    add(
        d,
        "GET",
        "/api/v1/business/customers/{customerId}/pets",
        "Listar pets do cliente",
        "Lista pets internos do cliente, sempre dentro do tenant autenticado.");
    add(
        d,
        "POST",
        "/api/v1/business/customers/{customerId}/pets",
        "Cadastrar pet do cliente",
        "Cria representação interna do pet para o cliente e tenant informados pelo contexto.");
    add(
        d,
        "GET",
        "/api/v1/business/pets/{id}",
        "Consultar pet do tenant",
        "Retorna dados internos do pet somente no tenant autenticado.");
    add(
        d,
        "PUT",
        "/api/v1/business/pets/{id}",
        "Atualizar pet do tenant",
        "Atualiza cadastro e observações internas do pet do tenant.");
    add(
        d,
        "DELETE",
        "/api/v1/business/pets/{id}",
        "Desativar pet do tenant",
        "Realiza exclusão lógica do pet interno.");
    catalogDocs(d, "products", "produto");
    catalogDocs(d, "services", "serviço");
    add(
        d,
        "GET",
        "/api/v1/business/availability-rules",
        "Listar regras de disponibilidade",
        "Lista regras semanais ativas do tenant autenticado.");
    add(
        d,
        "POST",
        "/api/v1/business/availability-rules",
        "Criar regra de disponibilidade",
        "Cria intervalo semanal de atendimento geral ou de um funcionário.");
    add(
        d,
        "DELETE",
        "/api/v1/business/availability-rules/{id}",
        "Desativar regra de disponibilidade",
        "Desativa uma regra semanal pertencente ao tenant.");
    add(
        d,
        "GET",
        "/api/v1/public/stores/{slug}/availability",
        "Consultar horários disponíveis",
        "Calcula slots livres no timezone da empresa, removendo intervalos já ocupados.");
    add(
        d,
        "GET",
        "/api/v1/business/appointments",
        "Listar agendamentos do tenant",
        "Lista paginada de agendamentos pertencentes ao tenant autenticado.");
    add(
        d,
        "POST",
        "/api/v1/business/appointments",
        "Criar agendamento empresarial",
        "Cria agendamento confirmado; duração é derivada do serviço e conflitos são protegidos no PostgreSQL.");
    appointmentAction(
        d, "confirm", "Confirmar agendamento", "Transiciona agendamento REQUESTED para CONFIRMED.");
    appointmentAction(
        d, "reject", "Rejeitar agendamento", "Transiciona agendamento REQUESTED para REJECTED.");
    appointmentAction(
        d, "cancel", "Cancelar agendamento", "Cancela agendamento REQUESTED ou CONFIRMED.");
    appointmentAction(
        d, "start", "Iniciar atendimento", "Transiciona agendamento CONFIRMED para IN_PROGRESS.");
    appointmentAction(
        d,
        "complete",
        "Concluir atendimento",
        "Transiciona agendamento IN_PROGRESS para COMPLETED.");
    appointmentAction(
        d, "no-show", "Registrar ausência", "Transiciona agendamento CONFIRMED para NO_SHOW.");
    add(
        d,
        "GET",
        "/api/v1/consumer/me/appointments",
        "Listar meus agendamentos",
        "Lista agendamentos vinculados ao ConsumerProfile autenticado.");
    add(
        d,
        "POST",
        "/api/v1/consumer/me/appointments",
        "Solicitar agendamento",
        "Solicita exatamente um slot público disponível para pet e cliente previamente vinculados à loja.");
    add(
        d,
        "POST",
        "/api/v1/consumer/me/appointments/{id}/cancel",
        "Cancelar meu agendamento",
        "Cancela somente um agendamento pertencente ao consumidor autenticado.");
    return Map.copyOf(d);
  }

  private static void catalogDocs(Map<String, EndpointDoc> d, String resource, String singular) {
    String base = "/api/v1/business/" + resource;
    add(
        d,
        "GET",
        base,
        "Listar " + resource,
        "Lista paginada de " + resource + " ativos pertencentes ao tenant autenticado.");
    add(
        d,
        "POST",
        base,
        "Criar " + singular,
        "Cria " + singular + " inicialmente não publicado no tenant autenticado.");
    add(
        d,
        "GET",
        base + "/{id}",
        "Consultar " + singular,
        "Consulta " + singular + " utilizando id e tenant_id.");
    add(
        d,
        "PUT",
        base + "/{id}",
        "Atualizar " + singular,
        "Atualiza " + singular + " e invalida o cache público aplicável.");
    add(
        d,
        "DELETE",
        base + "/{id}",
        "Desativar " + singular,
        "Executa exclusão lógica e remove o item da vitrine.");
    add(
        d,
        "POST",
        base + "/{id}/publish",
        "Publicar " + singular,
        "Publica o item ativo na vitrine e persiste evento na outbox.");
    add(
        d,
        "POST",
        base + "/{id}/unpublish",
        "Despublicar " + singular,
        "Remove o item da vitrine e invalida o cache público.");
  }

  private static void appointmentAction(
      Map<String, EndpointDoc> d, String action, String summary, String description) {
    add(d, "POST", "/api/v1/business/appointments/{id}/" + action, summary, description);
  }

  private static void add(
      Map<String, EndpointDoc> docs,
      String method,
      String path,
      String summary,
      String description) {
    docs.put(method + " " + path, new EndpointDoc(summary, description));
  }

  private static Map<String, String> parameterDescriptions() {
    return Map.ofEntries(
        Map.entry(
            "id",
            "UUID do recurso. Recursos multi-tenant são pesquisados juntamente com o tenant do JWT."),
        Map.entry(
            "slug",
            "Identificador público único da loja, usando letras minúsculas, números e hífens."),
        Map.entry("productId", "UUID do produto público pertencente à loja informada."),
        Map.entry("serviceId", "UUID do serviço pertencente ao tenant ou publicado pela loja."),
        Map.entry("customerId", "UUID do cliente dentro do CRM do tenant autenticado."),
        Map.entry(
            "employeeId",
            "UUID opcional do funcionário usado para filtrar disponibilidade ou reservar o recurso."),
        Map.entry("page", "Índice da página, iniciando em zero."),
        Map.entry("size", "Quantidade de elementos por página, entre 1 e 100."),
        Map.entry("date", "Data local da empresa no formato ISO-8601 YYYY-MM-DD."),
        Map.entry(
            "Idempotency-Key",
            "UUID único gerado pelo consumidor. Reutilize o mesmo valor ao repetir a mesma solicitação de upgrade; um corpo diferente gera 409."));
  }

  private static boolean isPublic(String path) {
    return path.startsWith("/api/v1/public/")
        || path.equals("/api/v1/auth/consumers/registrations");
  }

  private static Map<String, String> propertyDescriptions() {
    Map<String, String> p = new LinkedHashMap<>();
    p.put("id", "UUID público e não sequencial do recurso.");
    p.put("name", "Nome do recurso exibido nas operações correspondentes.");
    p.put("slug", "Slug público único da empresa.");
    p.put("tradeName", "Nome fantasia da empresa.");
    p.put("document", "Documento fiscal ou pessoal, sem autenticar identidade por si só.");
    p.put("description", "Descrição textual do recurso.");
    p.put("phone", "Telefone de contato.");
    p.put("email", "Endereço de e-mail válido.");
    p.put(
        "password",
        "Senha enviada somente ao provedor de identidade, com 12 a 128 caracteres; nunca é persistida ou registrada em log pela API.");
    p.put("termsAccepted", "Confirmação obrigatória de aceite dos termos da plataforma.");
    p.put("message", "Mensagem genérica que não revela se o endereço já possuía cadastro.");
    p.put("website", "URL pública da empresa.");
    p.put("publicVisible", "Indica se a empresa pode aparecer na vitrine pública.");
    p.put("appointmentApprovalMode", "Modo MANUAL ou AUTOMATIC para solicitações B2C.");
    p.put(
        "timezone",
        "Timezone IANA usado nas regras locais de agenda, por exemplo America/Sao_Paulo.");
    p.put("currency", "Código ISO-4217 de três letras, por exemplo BRL.");
    p.put("allowOnlineBooking", "Habilita consulta e solicitação online de horários.");
    p.put("allowOnlineSales", "Habilita vendas online na vitrine.");
    p.put("street", "Logradouro do endereço.");
    p.put("number", "Número ou complemento curto do endereço.");
    p.put("district", "Bairro do endereço.");
    p.put("city", "Cidade do endereço.");
    p.put("state", "Estado ou unidade federativa.");
    p.put("postalCode", "Código postal do endereço.");
    p.put("latitude", "Latitude decimal entre -90 e 90.");
    p.put("longitude", "Longitude decimal entre -180 e 180.");
    p.put("birthDate", "Data de nascimento no formato YYYY-MM-DD.");
    p.put("species", "Espécie do pet, por exemplo DOG ou CAT.");
    p.put("breed", "Raça do pet, quando conhecida.");
    p.put("sex", "Sexo do pet: MALE, FEMALE ou UNKNOWN.");
    p.put("weight", "Peso positivo do pet.");
    p.put("color", "Cor predominante do pet.");
    p.put("microchip", "Identificador do microchip, quando existente.");
    p.put("neutered", "Indica se o pet é castrado.");
    p.put("notes", "Observações internas ou instruções da operação.");
    p.put("consumerProfileId", "UUID opcional do perfil B2C vinculado após confirmação.");
    p.put("consumerPetId", "UUID do pet global pertencente ao consumidor autenticado.");
    p.put("customerId", "UUID do cliente do tenant.");
    p.put("petId", "UUID do pet interno do tenant.");
    p.put("sku", "Código de estoque único entre itens ativos do tenant.");
    p.put("price", "Preço monetário positivo com até duas casas decimais.");
    p.put("promotionalPrice", "Preço promocional positivo e não superior ao preço regular.");
    p.put("stockQuantity", "Quantidade disponível em estoque, igual ou maior que zero.");
    p.put("active", "Indica se o recurso está ativo e não foi excluído logicamente.");
    p.put("published", "Indica se o item está visível na vitrine pública.");
    p.put("durationMinutes", "Duração positiva do serviço em minutos.");
    p.put("requiresApproval", "Indica se o serviço requer aprovação do estabelecimento.");
    p.put(
        "employeeId",
        "UUID opcional do funcionário; ausente representa disponibilidade geral do serviço.");
    p.put("dayOfWeek", "Dia da semana ISO, de MONDAY a SUNDAY.");
    p.put("startLocal", "Horário local inicial no formato HH:mm:ss.");
    p.put("endLocal", "Horário local final no formato HH:mm:ss.");
    p.put("storeSlug", "Slug da loja pública selecionada pelo consumidor.");
    p.put("serviceId", "UUID do serviço selecionado.");
    p.put(
        "startAt",
        "Instante futuro em ISO-8601 UTC/offset; deve corresponder a um slot disponível para B2C.");
    p.put("endAt", "Fim calculado pelo backend a partir da duração do serviço.");
    p.put("status", "Estado atual centralizado do agendamento.");
    p.put("upgradeId", "UUID do workflow idempotente de upgrade para fornecedor.");
    p.put("tenantId", "Identificador do tenant criado pelo servidor; nunca é aceito do request.");
    p.put("businessId", "UUID público da empresa fornecedora criada.");
    p.put(
        "nextAction",
        "Próxima ação necessária para obter um token B2B após a conclusão do upgrade.");
    p.put("requestedBy", "Origem CUSTOMER ou BUSINESS do agendamento.");
    p.put("content", "Elementos da página atual.");
    p.put("page", "Índice da página atual, iniciado em zero.");
    p.put("size", "Quantidade solicitada de elementos por página.");
    p.put("totalElements", "Total de elementos disponíveis.");
    p.put("totalPages", "Total de páginas disponíveis.");
    return Map.copyOf(p);
  }

  private String propertyExample(String name) {
    return switch (name) {
      case "slug", "storeSlug" -> "clinica-pet-centro";
      case "email" -> "contato@example.com";
      case "password" -> "example-only-passphrase";
      case "phone" -> "+5511999999999";
      case "timezone" -> "America/Sao_Paulo";
      case "currency" -> "BRL";
      case "price" -> "129.90";
      case "promotionalPrice" -> "109.90";
      case "startAt" -> "2026-08-11T14:00:00Z";
      case "birthDate", "date" -> "2022-05-10";
      default -> null;
    };
  }

  private Object parameterExample(String name) {
    return switch (name) {
      case "page" -> 0;
      case "size" -> 20;
      case "slug" -> "clinica-pet-centro";
      case "date" -> "2026-08-11";
      case "id", "productId", "serviceId", "customerId", "employeeId" ->
          "550e8400-e29b-41d4-a716-446655440000";
      case "Idempotency-Key" -> "7d54213a-d336-4d70-bc8e-7be947060af7";
      default -> null;
    };
  }

  static Map<String, EndpointDoc> documentedEndpoints() {
    return ENDPOINTS;
  }

  record EndpointDoc(String summary, String description) {}
}
