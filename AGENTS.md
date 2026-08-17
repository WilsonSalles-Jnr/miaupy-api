# AGENTS.md

Este arquivo orienta agentes de IA/Codex ao trabalhar neste repositório.

---

## 1. Objetivo do projeto

Este projeto implementa uma plataforma SaaS multi-tenant para:

- clínicas veterinárias;
- banho e tosa;
- pet shops.

Também funciona como vitrine pública B2C.

Usuários B2C podem:

- manter seu perfil;
- cadastrar pets;
- visualizar empresas;
- visualizar produtos e serviços;
- adicionar produtos ao carrinho;
- realizar pedidos;
- solicitar agendamentos.

Usuários B2B podem:

- administrar sua empresa;
- cadastrar clientes;
- cadastrar pets;
- criar produtos;
- criar serviços;
- publicar itens;
- criar agendamentos;
- aceitar/rejeitar agendamentos;
- administrar pedidos.

A autenticação é fornecida por um projeto externo de Auth com suporte B2B e B2C.

---

# 2. Stack obrigatória

Ao gerar código novo, utilizar preferencialmente:

- Java 21;
- Spring Boot 3.x;
- Maven;
- Spring Web;
- Spring Validation;
- Spring Security Resource Server;
- Spring Data JPA;
- PostgreSQL;
- Liquibase;
- Kafka;
- Spring Kafka;
- Redis;
- JUnit 5;
- Mockito;
- Testcontainers.

Não trocar tecnologias principais sem necessidade explícita.

---

# 3. Arquitetura

O sistema deve ser tratado como um **monólito modular**.

Organizar código por feature/domínio e não por camada global.

Preferir:

```text
com.miaupy.catalog
com.miaupy.customer
com.miaupy.pet
com.miaupy.scheduling
com.miaupy.cart
com.miaupy.order
```

Evitar:

```text
com.miaupy.controller
com.miaupy.service
com.miaupy.repository
com.miaupy.dto
```

---

# 4. Estrutura de cada módulo

Preferir:

```text
feature
├── api
├── application
├── domain
└── infrastructure
```

Responsabilidades:

## api

- controllers;
- request DTOs;
- response DTOs;
- validação de entrada.

## application

- casos de uso;
- coordenação de transações;
- autorização contextual;
- publicação de eventos via outbox.

## domain

- regras de negócio;
- entidades/agregados;
- value objects;
- interfaces de repository;
- domain events.

## infrastructure

- JPA;
- Redis;
- Kafka;
- clients externos;
- implementações de repositories.

---

# 5. Regras multi-tenant

Esta é uma regra crítica.

Toda entidade pertencente a uma empresa deve possuir `tenantId`.

Exemplos:

- Product
- Service
- TenantCustomer
- TenantPet
- Appointment
- Order
- Employee

## Nunca confiar em tenantId vindo do body.

Incorreto:

```java
public record CreateProductRequest(
    Long tenantId,
    String name
) {}
```

Correto:

```java
public record CreateProductRequest(
    String name
) {}
```

O tenant deve ser obtido do token autenticado.

---

# 6. TenantContext

Criar/usar uma abstração central.

Exemplo conceitual:

```java
public interface TenantContext {
    Long getRequiredTenantId();
}
```

Controllers e casos de uso B2B devem usar o tenant autenticado.

Nenhuma query B2B pode esquecer o tenant.

---

# 7. Regra de segurança fundamental

Uma busca por ID de entidade multi-tenant deve utilizar:

```text
id + tenant_id
```

Nunca:

```text
findById(id)
```

quando a entidade pertence a um tenant.

Preferir:

```text
findByIdAndTenantId(id, tenantId)
```

ou equivalente no adapter de persistência.

---

# 8. B2C

O B2C é identificado pelo claim `sub`.

Nunca permitir que um consumidor selecione arbitrariamente outro consumerId.

Preferir endpoints:

```http
GET /api/v1/consumer/me
GET /api/v1/consumer/me/pets
```

em vez de:

```http
GET /api/v1/consumers/{consumerId}/pets
```

---

# 9. ConsumerProfile x TenantCustomer

Nunca unificar estas entidades.

## ConsumerProfile

Usuário global B2C da plataforma.

## TenantCustomer

Cadastro de CRM pertencente a uma empresa.

Relacionamento:

```text
TenantCustomer.consumerProfileId = nullable
```

Uma empresa pode cadastrar cliente sem que ele possua conta B2C.

---

# 10. ConsumerPet x TenantPet

Nunca usar um pet global como prontuário de todas as clínicas.

## ConsumerPet

Pet mantido pelo tutor B2C.

## TenantPet

Representação do pet dentro de uma empresa.

Pode existir:

```text
tenantPet.consumerPetId = nullable
```

Informações clínicas internas pertencem ao tenant.

---

# 11. JPA

Não expor entidade JPA em controller.

Não retornar:

```java
ResponseEntity<ProductJpaEntity>
```

Utilizar DTO.

Preferir `record` para DTOs imutáveis.

Exemplo:

```java
public record ProductResponse(
    UUID id,
    String name,
    BigDecimal price,
    boolean published
) {}
```

---

# 12. Transações

`@Transactional` deve ficar preferencialmente na camada de aplicação/use case.

Evitar controllers transacionais.

Operações que alteram agregado e geram evento devem persistir os dois na mesma transação via Outbox Pattern.

---

# 13. Kafka

Kafka é assíncrono.

Não usar Kafka para substituir operação CRUD simples.

Publicar eventos somente depois de uma mudança relevante no domínio.

Eventos típicos:

```text
appointment.requested
appointment.confirmed
appointment.cancelled
product.published
order.created
order.completed
customer.linked
pet.linked
```

---

# 14. Outbox Pattern

Eventos Kafka de domínio devem ser persistidos em tabela outbox.

Não fazer:

```java
repository.save(entity);
kafkaTemplate.send(...);
```

como única garantia de consistência.

Preferir:

```text
transaction
  save aggregate
  save outbox event
commit
```

Um publisher separado envia a outbox ao Kafka.

---

# 15. Eventos

Todo evento deve conter:

```text
eventId
eventType
eventVersion
occurredAt
tenantId quando aplicável
actor
payload
```

Exemplo:

```json
{
  "eventId": "uuid",
  "eventType": "order.created",
  "eventVersion": 1,
  "occurredAt": "2026-08-10T20:00:00Z",
  "tenantId": 50000101,
  "payload": {}
}
```

---

# 16. Consumidores Kafka

Consumers devem ser idempotentes.

Assuma que a mesma mensagem pode ser processada mais de uma vez.

Não criar lógica que gere cobrança, agendamento ou notificação duplicada sem proteção.

---

# 17. Redis

Redis pode ser utilizado para:

- cache;
- disponibilidade;
- locks;
- idempotência;
- rate limiting.

Redis não é a fonte primária para:

- pedidos;
- clientes;
- pets;
- agendamentos;
- carrinho.

PostgreSQL continua sendo a fonte de verdade.

---

# 18. Lock de agendamento

Ao implementar reserva de horário:

1. validar parâmetros;
2. obter lock curto no Redis;
3. iniciar transação;
4. validar conflito no PostgreSQL;
5. criar/alterar appointment;
6. criar outbox event;
7. commit;
8. liberar lock.

Não confiar exclusivamente no Redis.

---

# 19. Conflito de agenda

Uma reserva conflita quando:

```text
existing.start < requested.end
AND existing.end > requested.start
```

Considerar apenas status que efetivamente ocupam agenda.

Exemplo:

```text
REQUESTED
CONFIRMED
IN_PROGRESS
```

A regra de considerar `REQUESTED` pode ser parametrizada por tenant.

---

# 20. Status de agendamento

Utilizar enum:

```text
REQUESTED
CONFIRMED
REJECTED
CANCELLED
IN_PROGRESS
COMPLETED
NO_SHOW
```

Não utilizar strings mágicas.

Centralizar transições permitidas.

Exemplo:

```text
REQUESTED -> CONFIRMED
REQUESTED -> REJECTED
REQUESTED -> CANCELLED
CONFIRMED -> CANCELLED
CONFIRMED -> IN_PROGRESS
IN_PROGRESS -> COMPLETED
```

---

# 21. Carrinho

Regra do MVP:

**um carrinho ativo pertence a uma única empresa.**

Não implementar carrinho multi-vendedor sem requisito explícito.

Persistir carrinho em PostgreSQL.

Redis pode ser cache.

---

# 22. Pedido

Pedido deve armazenar snapshot de produto e preço.

Nunca recalcular pedido histórico usando o preço atual do catálogo.

OrderItem deve guardar pelo menos:

```text
productId
productName
quantity
unitPrice
total
```

---

# 23. Dinheiro

Usar:

```java
BigDecimal
```

Nunca utilizar `double` ou `float` para valores monetários.

Definir escala e arredondamento conscientemente.

---

# 24. Datas

Persistir instantes utilizando tipos apropriados.

Preferir:

```java
Instant
OffsetDateTime
```

para eventos globais.

Para regras locais de agenda, considerar timezone da empresa.

Toda empresa deve possuir timezone configurável.

---

# 25. IDs

Preferir UUID para recursos expostos externamente.

Não converter UUID para String internamente sem motivo.

---

# 26. Liquibase

Toda alteração de banco deve possuir migration.

Não depender de:

```text
hibernate.ddl-auto=update
```

em ambientes reais.

Preferir:

```text
validate
```

para Hibernate em produção.

---

# 27. Banco

Schemas recomendados:

```text
platform
consumer
crm
pet
catalog
scheduling
sales
integration
audit
```

Não criar tabelas aleatórias em `public` quando existir schema de domínio adequado.

---

# 28. Queries

Toda query multi-tenant deve filtrar `tenant_id`.

Sempre revisar índices de:

```text
tenant_id
tenant_id + status
tenant_id + created_at
tenant_id + active
```

dependendo do padrão de acesso.

---

# 29. Soft delete

Para dados históricos, preferir:

```text
active
deletedAt
```

Não excluir fisicamente:

- pedidos;
- agendamentos;
- histórico clínico.

---

# 30. API

Prefixos:

```text
/api/v1/public
/api/v1/consumer
/api/v1/business
```

## Public

Não exige autenticação quando apropriado.

## Consumer

Exige ator B2C.

## Business

Exige ator B2B + tenant.

---

# 31. Controllers

Controllers devem ser pequenos.

Responsabilidades:

- receber request;
- validar;
- extrair contexto;
- chamar use case;
- mapear response.

Não colocar regra de negócio no controller.

---

# 32. Services / Use Cases

Evitar classes `XxxService` gigantes.

Preferir casos de uso claros:

```text
CreateProductUseCase
PublishProductUseCase
RequestAppointmentUseCase
ConfirmAppointmentUseCase
CheckoutCartUseCase
```

---

# 33. Validação

Utilizar Bean Validation para formato.

Exemplos:

```text
@NotNull
@NotBlank
@Positive
@Email
@Size
```

Regras de domínio não devem ficar exclusivamente em annotations.

---

# 34. Exceptions

Criar exceptions de domínio/aplicação específicas.

Exemplos:

```text
ProductNotFoundException
AppointmentConflictException
InvalidAppointmentTransitionException
TenantAccessDeniedException
CartEmptyException
```

Mapear para `ProblemDetail`.

---

# 35. HTTP

Padrões:

```text
POST create -> 201
GET success -> 200
PUT/PATCH -> 200 ou 204
DELETE lógico -> 204
conflict -> 409
validation -> 400/422
```

Manter consistência no projeto.

---

# 36. Paginação

Listagens potencialmente grandes devem ser paginadas.

Nunca implementar:

```java
List<CustomerResponse> findAllCustomers();
```

sem limite em endpoint B2B de produção.

---

# 37. N+1

Sempre revisar associações JPA.

Evitar usar `EAGER` para corrigir N+1.

Preferir:

- query específica;
- projection;
- EntityGraph;
- fetch join quando apropriado.

---

# 38. Lombok

Lombok pode ser usado, mas evitar `@Data` indiscriminadamente em entidades JPA.

Preferir:

```text
@Getter
@NoArgsConstructor(PROTECTED)
```

e métodos de domínio explícitos.

---

# 39. Mappers

Evitar reflexão excessiva em domínio crítico.

Mapeamento explícito é preferível quando existir diferença entre:

- API DTO;
- domínio;
- JPA entity.

---

# 40. Logging

Não logar:

- senha;
- token completo;
- refresh token;
- dados de cartão;
- payload clínico sensível sem necessidade.

Logs devem incluir quando disponível:

```text
traceId
tenantId
actorId
eventId
```

---

# 41. Testes

Toda feature crítica deve incluir testes.

## Obrigatórios

### Multi-tenant

```text
tenant A cria recurso
tenant B tenta consultar
esperado: não encontra
```

### B2C

```text
consumer A cria pet
consumer B tenta consultar
esperado: não encontra
```

### Agenda

Testar duas requisições concorrentes para o mesmo horário.

### Kafka

Testar processamento duplicado.

### Outbox

Testar que mutation + evento são persistidos atomicamente.

---

# 42. Testcontainers

Para testes de integração utilizar containers reais de:

- PostgreSQL;
- Redis;
- Kafka.

Evitar H2 para validar comportamentos específicos do PostgreSQL.

---

# 43. Princípios de implementação

Ao receber uma tarefa:

1. identificar o bounded context;
2. verificar se a entidade é global ou tenant-scoped;
3. definir autorização;
4. definir regra de domínio;
5. definir persistência;
6. criar migration;
7. implementar use case;
8. implementar API;
9. criar testes;
10. revisar isolamento de tenant;
11. avaliar evento Kafka;
12. avaliar invalidação de cache Redis.

---

# 44. Antes de criar um novo módulo

Verificar se a responsabilidade já pertence a:

```text
business
consumer
customer
pet
catalog
scheduling
cart
order
notification
integration
audit
```

Não criar novo módulo apenas para uma classe.

---

# 45. Antes de criar uma dependência

Não adicionar biblioteca se Java/Spring já resolverem adequadamente.

Toda nova dependência deve ter justificativa prática.

---

# 46. Compatibilidade

Não atualizar versão de:

- Java;
- Spring Boot;
- Kafka;
- PostgreSQL;
- Redis;

durante uma tarefa não relacionada.

---

# 47. Alterações pequenas

Preferir mudanças pequenas e coesas.

Não refatorar todo o projeto ao implementar uma feature localizada.

---

# 48. Código morto

Não deixar:

- métodos comentados;
- classes duplicadas;
- TODO sem contexto;
- imports não utilizados;
- código experimental no fluxo principal.

---

# 49. Naming

Usar inglês no código.

Exemplos:

```text
Customer
Pet
Product
Service
Appointment
Order
```

Não misturar português e inglês em nomes de classes.

Documentação pode ser em português.

---

# 50. Convenções de endpoint

Usar substantivos.

Bom:

```http
POST /api/v1/business/products
```

Evitar:

```http
POST /api/v1/business/createProduct
```

Ações de mudança de estado podem utilizar subrecursos/ações claras:

```http
POST /api/v1/business/appointments/{id}/confirm
```

---

# 51. Segurança por padrão

Ao criar um endpoint novo, assuma que ele é privado até existir requisito explícito de endpoint público.

Itens públicos precisam respeitar:

```text
active = true
published = true
business.public_visible = true
```

---

# 52. Cache

Não adicionar cache sem definir:

- key;
- TTL;
- evento de invalidação;
- fallback para banco.

Quando produto publicado for alterado:

```text
invalidate store:{tenantId}:products
```

---

# 53. Não fazer

Não:

- confiar em tenantId do frontend;
- compartilhar prontuário entre tenants;
- usar Kafka como banco;
- usar Redis como fonte única;
- expor JPA entity;
- usar `double` para dinheiro;
- deixar endpoint sem autorização;
- fazer `findById` de entidade multi-tenant sem tenant;
- publicar Kafka sem estratégia de consistência;
- criar carrinho multi-vendedor no MVP;
- implementar microsserviço sem necessidade comprovada.

---

# 54. Commits e patches do agente

Ao modificar código:

- manter escopo da solicitação;
- preservar estilo existente;
- executar testes relevantes;
- reportar migrations criadas;
- reportar endpoints alterados;
- reportar novos tópicos Kafka;
- reportar novas chaves Redis;
- reportar riscos de compatibilidade.

---

# 55. Checklist final do agente

Antes de encerrar uma tarefa, responder internamente:

```text
[ ] A feature pertence ao módulo correto?
[ ] O tenant foi aplicado?
[ ] O B2C só acessa os próprios dados?
[ ] DTOs foram usados?
[ ] Migration foi criada?
[ ] Testes foram criados/ajustados?
[ ] Existe risco de N+1?
[ ] Existe risco de concorrência?
[ ] Precisa de evento Kafka?
[ ] Se usa Kafka, há idempotência?
[ ] Se publica evento de domínio, usa outbox?
[ ] Se usa Redis, existe fallback?
[ ] Cache é invalidado corretamente?
[ ] Erros HTTP estão coerentes?
[ ] Não há segredo ou dado sensível em log?
```

---

# 56. Fonte de verdade arquitetural

Quando houver dúvida de arquitetura, consultar primeiro:

```text
PROJECT_DOCUMENTATION.md
AGENTS.md
docs/adr/
```

Se a implementação existente divergir destes documentos, não fazer alteração estrutural ampla automaticamente.

Preservar compatibilidade e sinalizar a divergência.

---

# 57. Prioridade de desenvolvimento

Caso o agente seja solicitado a iniciar o projeto do zero, seguir aproximadamente:

```text
1. Spring Boot base
2. Docker Compose
3. Liquibase
4. JWT Resource Server
5. TenantContext
6. Business
7. ConsumerProfile
8. TenantCustomer
9. ConsumerPet
10. TenantPet
11. Product
12. Service
13. Store pública
14. Scheduling
15. Redis
16. Kafka
17. Outbox
18. Cart
19. Order
20. Notifications
```

---

# 58. Regra final

Priorize:

**segurança de tenant > consistência de dados > clareza de domínio > performance > conveniência de implementação.**

Quando uma solução mais curta violar isolamento de tenant, consistência transacional ou privacidade, não utilizá-la.
