# Cadastro B2C e cadastro empresarial direto

## Decisão arquitetural

A API Miaupy é uma fachada de onboarding e um Resource Server OAuth 2.0. Credenciais, login,
verificação de e-mail, bloqueio de força bruta e emissão de tokens pertencem ao provedor de
identidade. O ambiente local utiliza Keycloak; o navegador o acessa pelo proxy de mesmo domínio em
`http://localhost:3000/identity`, enquanto a aplicação acessa seu Admin REST API por um cliente técnico
de privilégio mínimo. A API nunca persiste senha e não deve registrar bodies de cadastro,
tokens ou segredos.

## Executar localmente

1. Em ambiente compartilhado, defina segredos diferentes dos defaults:

   ```powershell
   $env:KEYCLOAK_ADMIN_PASSWORD = "uma-senha-local-forte"
   $env:IDENTITY_CLIENT_SECRET = "um-segredo-local-longo-e-aleatorio"
   $env:REGISTRATION_KEY_SECRET = "outro-segredo-longo-e-aleatorio"
   $env:IDENTITY_PUBLIC_URL = "http://localhost:3000/identity"
   $env:IDENTITY_ADMIN_URL = "http://localhost:9000"
   ```

2. Execute `docker compose up -d` e inicie a API.

3. O login é servido em `http://localhost:3000/identity`, sem expor a porta interna do Keycloak ao
   usuário. Acesse a administração local diretamente em `http://localhost:9000`, Mailpit em
   `http://localhost:8025` e Swagger em `http://localhost:8080/swagger-ui.html`.

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
Authorization Code + PKCE no client `miaupy-consumer`. Na primeira chamada a
`GET /api/v1/consumer/me`, a API cria automaticamente o `ConsumerProfile` usando `sub`, `name` e
`email` do token verificado. O `PUT` permanece opcional para completar telefone, documento e data de
nascimento.

O link de verificação retorna para `FRONTEND_BASE_URL` e inicia o login do consumidor. Caso o
Keycloak solicite algum dado obrigatório de perfil, a conclusão volta ao callback da aplicação e
redireciona para `/conta`. As telas e e-mails do realm usam o locale `pt-BR`.

Os limites padrão são 10 tentativas por IP e 3 por e-mail em uma hora. As chaves Redis usam HMAC e
não contêm IP/e-mail em texto. A API usa o endereço remoto da conexão e não confia diretamente em
`X-Forwarded-For`. Em produção, configure os proxies confiáveis e proteção anti-bot adaptativa na
borda.

## Cadastro da empresa

O cadastro empresarial é independente do consumidor. Não exige conta B2C, JWT ou
`ConsumerProfile`. O responsável informa sua credencial e os dados iniciais do negócio em uma única
solicitação pública com `Idempotency-Key` UUID.

```http
POST /api/v1/auth/businesses/registrations
Idempotency-Key: 7d54213a-d336-4d70-bc8e-7be947060af7
Content-Type: application/json

{
  "ownerName": "Jane Doe",
  "email": "owner@example.com",
  "password": "uma frase secreta longa",
  "termsAccepted": true,
  "slug": "pet-store-centro",
  "name": "Pet Store Centro",
  "businessEmail": "contato@petstore.example"
}
```

O servidor aloca o tenant; `tenantId` nunca vem do request. Tenant, empresa, configurações e workflow
são persistidos numa transação. Depois, a identidade recebe `tenant_id` e `OWNER`, e a verificação é
enviada usando o client `miaupy-business`. Se o Keycloak falhar após a persistência local,
`LOCAL_READY` permanece e repetir a mesma chave e corpo retoma o fluxo. Chave/corpo incompatíveis
retornam `409`.

O endpoint sempre responde com mensagem genérica para não revelar se o e-mail já existe. Concluído
o fluxo e verificado o e-mail, o proprietário autentica no client `miaupy-business` e recebe token
com `actor_type=B2B`, `tenant_id` e `OWNER`.

Uma identidade B2B também pode comprar e solicitar serviços de outras empresas sem trocar de sessão.
As rotas de autosserviço em `/api/v1/consumer/me/**` aceitam tokens B2C ou B2B e vinculam carrinho,
pets, pedidos e agendamentos exclusivamente ao `sub` autenticado. O `tenant_id` do comprador nunca é
usado para acessar dados administrativos da empresa vendedora; `/api/v1/business/**` continua
restrito ao tenant do token.

Após a verificação, o link retorna ao login empresarial e o callback redireciona para `/empresa`.

## Checklist de produção

- HTTPS obrigatório para Keycloak e API;
- segredos em Secret Manager, rotacionados e diferentes dos defaults;
- Keycloak em modo de produção, hostname/TLS definidos e banco/usuário dedicado;
- SMTP real com SPF, DKIM e DMARC;
- redirect URIs e CORS restritos aos domínios reais;
- Authorization Code + PKCE, sem Resource Owner Password Grant;
- Keycloak publicado sob o mesmo domínio por proxy reverso, sem incorporar o formulário em iframe;
- `IDENTITY_PUBLIC_URL` e `AUTH_ISSUER_URI` alinhados com o domínio público `/identity`;
- MFA para proprietários e administradores;
- WAF/anti-bot, limites distribuídos e alertas de abuso;
- Admin Console/Admin REST API não expostos publicamente;
- auditoria de eventos administrativos, `429`, `503` e workflows incompletos;
- política LGPD, backups testados e teste de invasão antes do lançamento.

## Dados operacionais

- migrations: `005-onboarding.sql` e `010-direct-business-registration.sql`;
- evento outbox: `business.registered` no tópico de domínio existente;
- Redis: `onboarding:registration:ip:{hmac}` e `onboarding:registration:email:{hmac}`, TTL de 1 hora.
