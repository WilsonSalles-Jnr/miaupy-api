# Miaupy API

API de uma plataforma SaaS multi-tenant para clínicas veterinárias, banho e tosa e pet shops. O projeto segue a arquitetura definida em `PROJECT_DOCUMENTATION.md` e `AGENTS.md`: monólito modular, PostgreSQL como fonte de verdade e isolamento por `tenant_id` extraído exclusivamente do JWT.

## Estado atual

O projeto implementa a fundação e a Fase 2 (Empresa e CRM):

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

O body do perfil nunca contém `tenantId`; o valor é resolvido a partir do token.

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
