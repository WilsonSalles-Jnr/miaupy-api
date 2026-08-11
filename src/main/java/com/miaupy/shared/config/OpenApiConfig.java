package com.miaupy.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  public static final String BEARER_AUTH = "bearerAuth";

  @Bean
  OpenAPI miaupyOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Miaupy Platform API")
                .version("v1")
                .description(
                    "API multi-tenant para empresas do segmento pet e seus consumidores. "
                        + "Endpoints business obtêm o tenant exclusivamente do JWT; endpoints consumer "
                        + "obtêm o consumidor do claim sub.")
                .contact(new Contact().name("Miaupy Engineering"))
                .license(new License().name("Proprietary")))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_AUTH,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "Access token emitido pelo serviço externo de autenticação. "
                                + "B2B requer actor_type=B2B e tenant_id; B2C requer actor_type=B2C.")))
        .tags(
            List.of(
                tag("Empresa", "Perfil, endereço e configurações da empresa autenticada."),
                tag("Vitrine pública", "Informações públicas de lojas, produtos e serviços."),
                tag("Perfil B2C", "Perfil do consumidor autenticado, identificado pelo claim sub."),
                tag("Pets B2C", "Pets globais pertencentes ao consumidor autenticado."),
                tag("Clientes", "CRM de clientes isolado pelo tenant autenticado."),
                tag("Pets do tenant", "Pets e observações internas pertencentes ao tenant."),
                tag("Produtos", "Catálogo de produtos do tenant e publicação na vitrine."),
                tag("Serviços", "Catálogo de serviços do tenant e publicação na vitrine."),
                tag("Disponibilidade", "Regras semanais e consulta pública de horários."),
                tag("Agendamentos B2B", "Operação e transições de agendamentos do tenant."),
                tag("Agendamentos B2C", "Solicitações e consultas do consumidor autenticado.")));
  }

  private Tag tag(String name, String description) {
    return new Tag().name(name).description(description);
  }
}
