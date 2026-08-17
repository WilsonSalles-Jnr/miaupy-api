--liquibase formatted sql

--changeset miaupy:121-add-clinical-history-details
--validCheckSum: 9:1c612491c12f00cc18f487edf88328ff
ALTER TABLE clinical.history_event
    ADD COLUMN recorded_by_name VARCHAR(160) NOT NULL DEFAULT 'Usuário da empresa',
    ADD COLUMN details JSONB NOT NULL DEFAULT '{}'::jsonb;

UPDATE clinical.history_event history
SET recorded_by_name = profile.name
FROM consumer.consumer_profile profile
WHERE profile.auth_subject = history.recorded_by;

UPDATE clinical.history_event history
SET recorded_by_name = employee.name
FROM platform.employee employee
WHERE employee.tenant_id = history.tenant_id
  AND employee.auth_subject = history.recorded_by;

ALTER TABLE clinical.history_event
    ALTER COLUMN recorded_by_name DROP DEFAULT;

COMMENT ON COLUMN clinical.history_event.recorded_by_name IS
    'Snapshot do nome exibível do autor no momento do evento clínico.';
COMMENT ON COLUMN clinical.history_event.details IS
    'Snapshot JSON dos campos clínicos preenchidos no momento do evento.';
