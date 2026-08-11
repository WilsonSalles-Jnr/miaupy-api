# Miaupy API

API de uma plataforma SaaS multi-tenant para clínicas veterinárias, banho e tosa e pet shops. O projeto segue a arquitetura definida em `PROJECT_DOCUMENTATION.md` e `AGENTS.md`: monólito modular, PostgreSQL como fonte de verdade e isolamento por `tenant_id` extraído exclusivamente do JWT.

## Estado atual

O projeto implementa a fundação e as Fases 2 a 4 (CRM, Catálogo e Agenda):

- Java 21 e Spring Boot 3;
- Resource Server JWT para atores B2B/B2C;
- `TenantContext` central baseado nos claims `actor_type` e `tenant_id`;
- respostas de erro em `ProblemDetail` e correlation ID;
- PostgreSQL, Liquibase, Redis e Kafka configurados;
- Docker Compose para desenvolvimento;
- perfil privado da empresa e consulta da vitrine pública;
- endereço, timezone, moeda e configurações operacionais da empresa;
- perfil B2C derivado do claim `sub` e pets próprios do consumidor;
- CRM de clientes e pets internos isolados por tenant;
- soft delete e optimistic locking nos dados operacionais;
- catálogo multi-tenant de produtos e serviços;
- publicação explícita e vitrine pública paginada;
- cache Redis com TTL, invalidação versionada e fallback para PostgreSQL;
- eventos de catálogo persistidos na outbox na mesma transação da mutação;
- regras semanais de disponibilidade no timezone da empresa;
- agendamentos B2B e solicitações B2C com transições de estado centralizadas;
- proteção contra sobreposição com lock Redis e exclusion constraint PostgreSQL;
- publisher Kafka da outbox com entrega at-least-once;
- teste multi-tenant unitário e teste PostgreSQL com Testcontainers.

## Ambiente local

Pré-requisitos: Java 21, Docker e Docker Compose.

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
```

As configurações aceitam as variáveis `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `KAFKA_BOOTSTRAP_SERVERS` e `AUTH_JWK_SET_URI`. Os valores padrão são exclusivamente locais.

## JWT esperado

Ator B2B:

```json
{
  "sub": "user-uuid",
  "actor_type": "B2B",
  "tenant_id": 50000101,
  "roles": ["OWNER"]
}
```

Ator B2C:

```json
{
  "sub": "consumer-uuid",
  "actor_type": "B2C"
}
```

## Endpoints implementados

| Método | Endpoint | Acesso |
|---|---|---|
| `POST` | `/api/v1/business/profile` | B2B `OWNER` ou `ADMIN` |
| `GET` | `/api/v1/business/profile` | B2B autenticado |
| `PUT` | `/api/v1/business/profile` | B2B `OWNER` ou `ADMIN` |
| `GET` | `/api/v1/public/stores/{slug}` | Público; somente empresa ativa e visível |
| `GET/PUT` | `/api/v1/business/settings` | B2B; alteração por `OWNER` ou `ADMIN` |
| `GET/PUT` | `/api/v1/business/address` | B2B; alteração por `OWNER` ou `ADMIN` |
| `GET/PUT` | `/api/v1/consumer/me` | B2C identificado pelo `sub` |
| CRUD | `/api/v1/consumer/me/pets` | B2C; somente pets próprios |
| CRUD | `/api/v1/business/customers` | B2B; sempre filtrado por tenant |
| CRUD | `/api/v1/business/customers/{customerId}/pets` e `/api/v1/business/pets/{id}` | B2B; sempre filtrado por tenant |
| CRUD | `/api/v1/business/products` | B2B; sempre filtrado por tenant |
| `POST` | `/api/v1/business/products/{id}/publish` e `/unpublish` | B2B com permissão de catálogo |
| CRUD | `/api/v1/business/services` | B2B; sempre filtrado por tenant |
| `POST` | `/api/v1/business/services/{id}/publish` e `/unpublish` | B2B com permissão de catálogo |
| `GET` | `/api/v1/public/stores/{slug}/products` | Público; somente ativos e publicados |
| `GET` | `/api/v1/public/stores/{slug}/products/{productId}` | Público; somente ativo e publicado |
| `GET` | `/api/v1/public/stores/{slug}/services` | Público; somente ativos e publicados |
| `GET/POST/DELETE` | `/api/v1/business/availability-rules` | B2B; regras semanais por tenant |
| `GET/POST` | `/api/v1/business/appointments` | B2B; listagem e criação |
| `POST` | `/api/v1/business/appointments/{id}/{action}` | B2B; confirmar, rejeitar, cancelar, iniciar, concluir ou marcar ausência |
| `GET/POST` | `/api/v1/consumer/me/appointments` | B2C; recursos derivados do `sub` |
| `POST` | `/api/v1/consumer/me/appointments/{id}/cancel` | B2C; somente agendamento próprio |
| `GET` | `/api/v1/public/stores/{slug}/availability` | Público; horários disponíveis por serviço e data |

O body do perfil nunca contém `tenantId`; o valor é resolvido a partir do token.

O cache público usa as chaves versionadas `store:{tenantId}:products:*` e
`store:{tenantId}:services:*`. O TTL padrão é de cinco minutos e pode ser alterado com
`PUBLIC_CATALOG_CACHE_TTL`.

O lock de agendamento usa `appointment:lock:{tenantId}:{resource}:{startAt}` com TTL de 15
segundos. A proteção definitiva é a constraint de exclusão PostgreSQL para intervalos
`[startAt,endAt)` nos estados `REQUESTED`, `CONFIRMED` e `IN_PROGRESS`.

Os eventos da outbox são publicados no tópico `miaupy.domain-events`, configurável por
`DOMAIN_EVENTS_TOPIC`. A entrega é at-least-once; consumers devem permanecer idempotentes.

## Testes

```powershell
.\mvnw.cmd test
```

O teste de integração usa PostgreSQL real e é ignorado automaticamente quando o Docker não está disponível. H2 não é utilizado.

## Formatação

O código Java segue `google-java-format`. Para formatar e verificar:

```powershell
.\mvnw.cmd fmt:format
.\mvnw.cmd verify
```
