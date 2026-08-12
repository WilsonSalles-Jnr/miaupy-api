# Notificações

O primeiro incremento da Fase 6 processa eventos do tópico `miaupy.domain-events` e cria
notificações persistentes antes da entrega. O PostgreSQL é a fonte de verdade e a tabela
`integration.processed_event` impede o processamento duplicado do mesmo evento Kafka.

## Eventos aceitos

Somente eventos da allowlist interna podem produzir comunicação externa:

- `appointment.requested` notifica a empresa;
- `appointment.confirmed`, `appointment.rejected`, `appointment.cancelled` e
  `appointment.completed` notificam o cliente;
- `order.created` notifica cliente e empresa;
- `order.processing`, `order.ready`, `order.cancelled` e `order.completed` notificam o cliente.

Eventos desconhecidos e `notification.requested` não são usados para enviar conteúdo arbitrário.
Assunto, corpo e destinatário são montados pelo backend a partir dos dados persistidos.

## E-mail

O canal ativo é SMTP. No ambiente local, o padrão aponta para o Mailpit:

```text
SMTP: localhost:1025
Interface: http://localhost:8025
Remetente: no-reply@miaupy.local
```

Variáveis disponíveis:

```text
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
MAIL_SMTP_AUTH
MAIL_STARTTLS
NOTIFICATION_FROM
NOTIFICATION_MAX_ATTEMPTS
NOTIFICATION_DELIVERY_DELAY
```

Falhas usam backoff exponencial e são encerradas após o número máximo de tentativas. Logs contêm
somente o identificador da notificação, canal e tentativa; destinatário e conteúdo não são
registrados.

## Lembretes

Agendamentos confirmados recebem lembrete por e-mail, por padrão 24 horas antes. O horário é
formatado no timezone configurado pela empresa. A chave de deduplicação inclui agendamento e
horário de início, evitando lembretes duplicados.

```text
APPOINTMENT_REMINDER_LEAD=PT24H
APPOINTMENT_REMINDER_WINDOW=PT5M
APPOINTMENT_REMINDER_SCAN_DELAY=PT5M
```

## Push e WhatsApp

Os canais `PUSH` e `WHATSAPP` e o contrato `NotificationDelivery` já estão modelados. Eles não são
ativados sem um provedor escolhido, credenciais armazenadas com segurança, consentimento do usuário
e política de opt-out. Essa integração pertence ao próximo incremento da Fase 6.

## APIs

```http
GET /api/v1/consumer/me/notifications
GET /api/v1/business/notifications
```

As consultas são paginadas e filtradas respectivamente pelo `ConsumerProfile` e pelo `tenantId` do
JWT. O endereço utilizado para entrega não é exposto nas respostas.
