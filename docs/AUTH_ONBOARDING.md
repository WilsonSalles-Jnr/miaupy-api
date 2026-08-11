# Cadastro B2C e upgrade para fornecedor

## Decisão arquitetural

A API Miaupy é uma fachada de onboarding e um Resource Server OAuth 2.0. Credenciais, login,
verificação de e-mail, bloqueio de força bruta e emissão de tokens pertencem ao provedor de
identidade. O ambiente local utiliza Keycloak; a aplicação acessa seu Admin REST API por um cliente
técnico de privilégio mínimo. A API nunca persiste senha e não deve registrar bodies de cadastro,
tokens ou segredos.

## Executar localmente

1. Em ambiente compartilhado, defina segredos diferentes dos defaults:

   ```powershell
   $env:KEYCLOAK_ADMIN_PASSWORD = "uma-senha-local-forte"
   $env:IDENTITY_CLIENT_SECRET = "um-segredo-local-longo-e-aleatorio"
   $env:REGISTRATION_KEY_SECRET = "outro-segredo-longo-e-aleatorio"
   ```

2. Execute `docker compose up -d` e inicie a API.

3. Acesse Keycloak em `http://localhost:9000`, Mailpit em `http://localhost:8025` e Swagger em
   `http://localhost:8080/swagger-ui.html`.

O realm `miaupy` é importado somente na primeira inicialização. Alterar o JSON não sobrescreve um
realm persistido; aplique mudanças administrativamente ou recrie apenas o ambiente local quando a
perda de dados de desenvolvimento for aceitável.

## Cadastro do consumidor

```http
POST /api/v1/auth/consumers/registrations
Content-Type: application/json

{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "uma frase secreta longa",
  "termsAccepted": true
}
```

O endpoint retorna `202 Accepted` com a mesma mensagem se criar a conta ou se o e-mail já existir,
evitando enumeração direta. O Keycloak envia a verificação ao Mailpit. Após verificá-la, o cliente usa
Authorization Code + PKCE no client `miaupy-consumer`. `PUT /api/v1/consumer/me` cria o
`ConsumerProfile` vinculado ao claim `sub`.

Os limites padrão são 10 tentativas por IP e 3 por e-mail em uma hora. As chaves Redis usam HMAC e
não contêm IP/e-mail em texto. A API usa o endereço remoto da conexão e não confia diretamente em
`X-Forwarded-For`. Em produção, configure os proxies confiáveis e proteção anti-bot adaptativa na
borda.

## Upgrade para fornecedor

Exige JWT do client `miaupy-consumer`, `actor_type=B2C`, `email_verified=true`, perfil existente e um
`Idempotency-Key` UUID.

```http
POST /api/v1/consumer/me/provider-upgrades
Authorization: Bearer <token-b2c>
Idempotency-Key: 7d54213a-d336-4d70-bc8e-7be947060af7
Content-Type: application/json

{
  "slug": "pet-store-centro",
  "name": "Pet Store Centro",
  "email": "owner@example.com"
}
```

O servidor aloca o tenant; `tenantId` nunca vem do request. Um lock PostgreSQL por `sub` serializa
concorrência. Tenant, empresa, configurações e workflow são persistidos numa transação. Depois, a
identidade recebe `tenant_id` e `OWNER`. Se o Keycloak falhar, `LOCAL_READY` permanece e repetir a
mesma chave e corpo retoma o fluxo. Chave/corpo incompatíveis retornam `409`.

Concluído o fluxo, o usuário autentica no client `miaupy-business` e recebe token com
`actor_type=B2B`, `tenant_id` e `OWNER`. Ele continua usando o client B2C nas operações pessoais; o
perfil global e a empresa continuam entidades separadas.

## Checklist de produção

- HTTPS obrigatório para Keycloak e API;
- segredos em Secret Manager, rotacionados e diferentes dos defaults;
- Keycloak em modo de produção, hostname/TLS definidos e banco/usuário dedicado;
- SMTP real com SPF, DKIM e DMARC;
- redirect URIs e CORS restritos aos domínios reais;
- Authorization Code + PKCE, sem Resource Owner Password Grant;
- MFA para proprietários e administradores;
- WAF/anti-bot, limites distribuídos e alertas de abuso;
- Admin Console/Admin REST API não expostos publicamente;
- auditoria de eventos administrativos, `429`, `503` e workflows incompletos;
- política LGPD, backups testados e teste de invasão antes do lançamento.

## Dados operacionais

- migration: `005-onboarding.sql`;
- evento outbox: `provider.upgraded` no tópico de domínio existente;
- Redis: `onboarding:registration:ip:{hmac}` e `onboarding:registration:email:{hmac}`, TTL de 1 hora.
