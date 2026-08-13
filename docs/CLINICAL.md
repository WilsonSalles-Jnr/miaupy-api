# Módulo clínico

O módulo clínico pertence exclusivamente ao contexto B2B. Todo prontuário é associado ao
`TenantPet`, nunca diretamente ao `ConsumerPet`. Todas as consultas usam `tenant_id + tenant_pet_id`
obtidos do JWT e da rota validada pelo backend.

Dados clínicos não são compartilhados automaticamente com outro tenant nem expostos nas APIs B2C.
Os endpoints exigem um usuário com papel `OWNER`, `ADMIN` ou `VETERINARIAN`.

## Recursos

- prontuário com alergias, condições crônicas, medicamentos e observações;
- consultas e vínculo opcional com um agendamento do mesmo pet;
- vacinações e próxima dose;
- receitas e vínculo opcional com uma consulta;
- anexos PDF, JPEG e PNG;
- histórico clínico imutável e paginado.

## Anexos

O primeiro incremento persiste anexos no PostgreSQL para manter a implantação local simples e
transacional. São aplicadas as seguintes proteções:

- máximo de 10 MB;
- allowlist de `application/pdf`, `image/jpeg` e `image/png`;
- validação da assinatura binária, sem confiar apenas no `Content-Type`;
- sanitização do nome do arquivo;
- hash SHA-256;
- download com `Content-Disposition: attachment` e `X-Content-Type-Options: nosniff`;
- busca obrigatória por anexo, pet e tenant;
- conteúdo e informações clínicas não são registrados em log.

Para arquivos maiores ou escala elevada, o próximo passo é usar object storage privado com
criptografia, antivírus, URLs assinadas curtas e política definida de retenção. O banco deve manter
os metadados e a associação ao tenant.

## Auditoria e consistência

Registros guardam o `sub` do autor autenticado. Cada mutação cria um evento resumido no histórico e
um evento via outbox na mesma transação. O payload da outbox contém apenas identificadores, sem
diagnóstico, receita ou outros dados clínicos sensíveis.

O prontuário usa optimistic locking. Consultas, vacinas, receitas e eventos de histórico são
imutáveis neste incremento; correções futuras devem ser feitas por adendo auditável, não por
exclusão do histórico.

## Endpoints

```http
GET  /api/v1/business/pets/{petId}/clinical/medical-record
PUT  /api/v1/business/pets/{petId}/clinical/medical-record
POST /api/v1/business/pets/{petId}/clinical/consultations
POST /api/v1/business/pets/{petId}/clinical/vaccinations
POST /api/v1/business/pets/{petId}/clinical/prescriptions
POST /api/v1/business/pets/{petId}/clinical/attachments
GET  /api/v1/business/pets/{petId}/clinical/attachments/{attachmentId}/content
GET  /api/v1/business/pets/{petId}/clinical/history
```
